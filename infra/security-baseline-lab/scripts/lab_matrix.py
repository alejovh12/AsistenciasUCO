"""SEC-BASELINE-001/002 — matriz HTTP real contra el laboratorio (solo GET / decisiones, sin escrituras).

Uso:  python -I lab_matrix.py pep                  -> decisiones reales PEP/PDP (sin backend)
      python -I lab_matrix.py backend [url]        -> backend 8083 en IdentityMode Institutional
      python -I lab_matrix.py backend-pep-down [url]
      python -I lab_matrix.py e2e [url]            -> SEC-BASELINE-002, backend 8083 en IdentityMode SharedLab
      python -I lab_matrix.py e2e-pep-down [url]   -> idem con el contenedor pep detenido
      python -I lab_matrix.py e2e-fault [url]      -> idem con SECURITY_BASELINE_PEP_URL hacia pep_fault_stub.py
      python -I lab_matrix.py e2e-pdp-down [url]   -> SEC-BASELINE-003, PEP arriba y contenedor pdp detenido
      python -I lab_matrix.py stable [url]         -> SEC-BASELINE-003, mismo jar con PEP OFF e identidad institucional
      python -I lab_matrix.py selftest             -> pruebas del propio arnés (sin red)

Veredictos: PASS; FAIL (fallo propio); KNOWN_UPSTREAM_FAIL (defecto conocido del proveedor, nunca PASS).
Exit code: 0 todo PASS; 1 al menos un FAIL propio; 2 solo fallos upstream conocidos.
Una decisión ALLOW del PEP solo es PASS con HTTP 204 y un X-Decision-Id válido.

Los tokens se obtienen en memoria y nunca se imprimen ni se escriben. El env institucional se lee
en solo lectura desde infra/keycloak/.env del checkout estable (no se copia al laboratorio).
"""
import http.client
import json
import os
import re
import sys
import tempfile
import time
import urllib.parse
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402

INSTITUTIONAL_ENV = os.environ.get(
    "ASISTENCIAS_INSTITUTIONAL_ENV",
    r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\keycloak\.env")
INSTITUTIONAL_KC = "http://127.0.0.1:8081"
BACKEND_LOG = os.environ.get(
    "ASISTENCIAS_LAB_BACKEND_LOG",
    os.path.join(tempfile.gettempdir(), "asistencias-security-lab", "backend-pep-lab.jsonl"))
VERIFICATION = os.path.join(os.path.dirname(L.ENV_PATH), "academic-identity-verification.json")
DECISION_ID = re.compile(r"[A-Za-z0-9_.:-]{1,128}")

PASS, FAIL, KNOWN = "PASS", "FAIL", "KNOWN_UPSTREAM_FAIL"
# Defectos del proveedor Security Baseline v0.1.6 ya reportados. Siguen contando como fallo (exit 2).
KNOWN_UPSTREAM = {
    "PEP-03": "PEP v0.1.6 responde 200 sin X-Decision-Id cuando falta el Bearer (fail-open); mitigado en el cliente",
}

E2E_CLIENT = ("asistencias-lab-e2e", "LAB_E2E_CLIENT_SECRET")
WRONG_AUD_CLIENT = ("asistencias-lab-e2e-wrong-aud", "LAB_E2E_WRONG_AUD_CLIENT_SECRET")
GROUP_OWN = "de000005-0000-4000-8000-000000000001"      # FIX-PAG-300
GROUP_FOREIGN = "de000005-0000-4000-8000-000000000002"  # FIX-PAG-1000
OWN_ID_USUARIO = "de000003-0000-4000-8000-000000000001"

# Capas responsables (columna "layer").
L_JWT = "Spring Resource Server / JwtDecoder"
L_ENTRY = "Spring Resource Server / sin credencial"
L_PEP = "PEP/PDP deny"
L_ROLE = "Spring AuthorizationFilter (rol local)"
L_SCOPE = "Application / InstitutionalScopePort (titularidad)"
L_UNAVAILABLE = "PEP client fail-closed"
L_ALLOW = "PEP/PDP allow + Spring + Application"
L_PUBLIC = "Ruta pública (sin PEP)"

results = []


# --------------------------------------------------------------------------- utilidades

def read_dotenv(path):
    values = {}
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip().strip('"')
    return values


def institutional_token(username_key, password_key):
    env = read_dotenv(INSTITUTIONAL_ENV)
    realm = env.get("KC_REALM", "asistencias-uco")
    client = env.get("KC_FRONTEND_CLIENT_ID", "asistencias-uco-frontend")
    username = env.get(username_key, username_key)
    status, _, raw = L.http(L.plain_opener(), "POST",
                            f"{INSTITUTIONAL_KC}/realms/{realm}/protocol/openid-connect/token",
                            {"grant_type": "password", "client_id": client,
                             "username": username, "password": env[password_key], "scope": "openid"}, form=True)
    if status != 200:
        raise SystemExit(f"institutional token {username_key}: HTTP {status}")
    return json.loads(raw)["access_token"]


def lab_token(env, username, client=E2E_CLIENT):
    client_id, secret_key = client
    key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
    status, _, raw = L.http(L.plain_opener(), "POST",
                            f"{L.KEYCLOAK}/realms/{L.REALM}/protocol/openid-connect/token",
                            {"grant_type": "password", "client_id": client_id, "client_secret": env[secret_key],
                             "username": username, "password": env[key], "scope": "openid"}, form=True)
    if status != 200:
        raise SystemExit(f"lab token {client_id}/{username}: HTTP {status}")
    return json.loads(raw)["access_token"]


def tamper(token):
    head, payload, signature = token.split(".")
    flipped = ("A" if signature[0] != "A" else "B") + signature[1:]
    return f"{head}.{payload}.{flipped}"


def summarize_claims(token):
    claims = L.jwt_claims(token)
    keep = {k: claims.get(k) for k in ("iss", "aud", "azp", "tenant")}
    keep["sub_present"] = bool(claims.get("sub"))
    keep["idUsuario_present"] = "idUsuario" in claims
    keep["asistencias_api_roles"] = (claims.get("resource_access") or {}).get("asistencias-api", {}).get("roles")
    return keep


def valid_decision_id(value):
    return value is not None and DECISION_ID.fullmatch(value) is not None


def lower_headers(headers):
    return {k.lower(): v for k, v in (headers or {}).items()}


def parse_body(body):
    try:
        return json.loads(body or b"{}")
    except ValueError:
        return {}


def backend_log_has(correlation_id, needle):
    """True si el log JSON del backend registra `needle` para ese correlation ID."""
    if not correlation_id or not os.path.exists(BACKEND_LOG):
        return False
    with open(BACKEND_LOG, encoding="utf-8", errors="replace") as handle:
        return any(correlation_id in line and needle in line for line in handle)


# --------------------------------------------------------------------------- veredicto

def verdict_for(case, expected, status, allow=False, decision_id=None, checks=()):
    """Veredicto puro (probado en selftest). `checks` son (descripción, bool) adicionales."""
    allowed = [str(e) for e in (expected if isinstance(expected, (list, tuple)) else [expected])]
    problems = []
    if str(status) not in allowed:
        problems.append(f"status {status} not in {allowed}")
    if allow:
        if status != 204:
            problems.append("ALLOW exige HTTP 204")
        if not valid_decision_id(decision_id):
            problems.append("ALLOW exige X-Decision-Id válido")
    problems += [description for description, ok in checks if not ok]
    if not problems:
        return PASS, problems
    key = case.split(" ", 1)[0]
    return (KNOWN if key in KNOWN_UPSTREAM else FAIL), problems


def record(case, expected, status, headers, body, extra="", allow=False, layer=None, count=None, checks=(),
           decision_id=None):
    parsed = parse_body(body)
    code = parsed.get("code") or parsed.get("title") if isinstance(parsed, dict) else None
    lower = lower_headers(headers)
    decision = decision_id if decision_id is not None else lower.get("x-decision-id")
    verdict, problems = verdict_for(case, expected, status, allow=allow, decision_id=lower.get("x-decision-id"),
                                    checks=checks)
    correlation = lower.get("x-correlation-id") or (parsed.get("correlationId") if isinstance(parsed, dict) else None)
    note = extra
    key = case.split(" ", 1)[0]
    if verdict == KNOWN:
        note = (note + "; " if note else "") + KNOWN_UPSTREAM[key]
    if problems:
        note = (note + "; " if note else "") + "; ".join(problems)
    row = {"case": case, "expected": expected, "status": status, "code": code, "correlationId": correlation,
           "decisionId": decision, "layer": layer, "count": count, "verdict": verdict, "note": note}
    results.append(row)
    print(json.dumps(row, ensure_ascii=False))
    return row


# --------------------------------------------------------------------------- HTTP

def pep_decision(credential, bearer, path, method="GET", app="asistencias-uco-lab", environment="local"):
    headers = {"X-Correlation-Id": f"sec-lab-{uuid.uuid4()}", "Accept": "application/json"}
    if credential is not None:
        headers["X-Application-Credential"] = credential
    if bearer is not None:
        headers["Authorization"] = f"Bearer {bearer}"
    return L.http(L.plain_opener(), "POST", f"{L.PEP}/internal/v1/embedded-access-decisions",
                  {"applicationName": app, "environment": environment, "path": path, "method": method},
                  headers=headers)


def backend_get(base, path, bearer=None, extra_headers=None):
    headers = {"X-Correlation-Id": str(uuid.uuid4()), "Accept": "application/json"}
    if bearer is not None:
        headers["Authorization"] = f"Bearer {bearer}"
    headers.update(extra_headers or {})
    return L.http(L.plain_opener(), "GET", f"{base}{path}", headers=headers)


def sse_open(base, path, bearer=None, read_seconds=2.0, header_seconds=35.0):
    """Abre el stream SSE, lee cabeceras y hasta `read_seconds` de cuerpo, y cierra. Sin seguir redirects.

    El backend MVC no confirma las cabeceras de un stream permitido hasta el primer heartbeat
    (RealtimeEventsController.HEARTBEAT_INTERVAL = 25 s); por eso se espera hasta `header_seconds`.
    """
    target = urllib.parse.urlsplit(base)
    connection = http.client.HTTPConnection(target.hostname, target.port, timeout=header_seconds)
    headers = {"Accept": "text/event-stream", "X-Correlation-Id": str(uuid.uuid4())}
    if bearer is not None:
        headers["Authorization"] = f"Bearer {bearer}"
    try:
        connection.request("GET", path, headers=headers)
        response = connection.getresponse()
        status, response_headers = response.status, dict(response.getheaders())
        body = b""
        if status != 200:
            body = response.read()
        else:
            connection.sock.settimeout(read_seconds)
            try:
                body = response.read1(4096) if hasattr(response, "read1") else response.read(1)
            except (TimeoutError, OSError):
                body = b""
        return status, response_headers, body
    finally:
        connection.close()


def active_subscribers(base):
    """Gauge realtime.subscribers.active de /actuator/prometheus (ruta pública, sin PEP). None si no está."""
    status, _, raw = L.http(L.plain_opener(), "GET", f"{base}/actuator/prometheus", headers={"Accept": "text/plain"})
    if status != 200:
        return None
    for line in (raw or b"").decode("utf-8", errors="replace").splitlines():
        if line.startswith("realtime_subscribers_active") and not line.startswith("#"):
            return float(line.rsplit(" ", 1)[1])
    return None


def json_error_checks(headers, body, code, status):
    """Cuerpo ApiErrorResponse JSON con el code/status contractual (FIX-001)."""
    parsed = parse_body(body)
    return [("Content-Type application/json", "application/json" in lower_headers(headers).get("content-type", "")),
            (f"code {code}", isinstance(parsed, dict) and parsed.get("code") == code),
            (f"status {status} en el cuerpo", isinstance(parsed, dict) and parsed.get("status") == status)]


def items(body):
    parsed = parse_body(body)
    if isinstance(parsed, dict):
        return parsed.get("datos") or parsed.get("items") or []
    return []


def load_expected():
    with open(VERIFICATION, encoding="utf-8-sig") as handle:
        evidence = json.load(handle)
    return {i["idUsuario"]: i for i in evidence["identities"]}


# --------------------------------------------------------------------------- modos SEC-BASELINE-001

def run_pep():
    env = L.read_env()
    credential = env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL"]
    allowed = L.password_grant(env, "lab-docente-permitido", "LAB_USER_LAB_DOCENTE_PERMITIDO_PASSWORD")
    denied = L.password_grant(env, "lab-docente-sin-permiso", "LAB_USER_LAB_DOCENTE_SIN_PERMISO_PASSWORD")
    print("lab token claims:", json.dumps(summarize_claims(allowed)))
    institutional = institutional_token("E2E_DOCENTE_USERNAME", "E2E_DOCENTE_PASSWORD")
    print("institutional token claims:", json.dumps(summarize_claims(institutional)))

    record("PEP-01 sin credencial de aplicación", 401, *pep_decision(None, allowed, "/api/v1/docente/horarios"))
    record("PEP-02 credencial de aplicación incorrecta", 401,
           *pep_decision("x" * len(credential), allowed, "/api/v1/docente/horarios"))
    record("PEP-03 sin Bearer", 401, *pep_decision(credential, None, "/api/v1/docente/horarios"))
    record("PEP-04 Bearer con firma alterada", 401, *pep_decision(credential, tamper(allowed), "/api/v1/docente/horarios"))
    record("PEP-05 Escenario A: JWT institucional (issuer :8081)", 401,
           *pep_decision(credential, institutional, "/api/v1/docente/horarios"))
    record("PEP-06 lab token con rol PDP sobre recurso concedido", 204,
           *pep_decision(credential, allowed, "/api/v1/docente/horarios"), allow=True)
    record("PEP-07 lab token sin asignación PDP", 403, *pep_decision(credential, denied, "/api/v1/docente/horarios"))
    record("PEP-08 recurso registrado no concedido al rol", 403,
           *pep_decision(credential, allowed, "/api/v1/estudiantes"))
    record("PEP-09 ruta no registrada", 403, *pep_decision(credential, allowed, "/api/v1/admin/sedes"))
    record("PEP-10 método distinto al concedido", 403,
           *pep_decision(credential, allowed, "/api/v1/docente/horarios", method="POST"))
    record("PEP-11 SSE concedido", 204, *pep_decision(credential, allowed, "/api/v1/realtime/stream"), allow=True)
    record("PEP-12 SSE sin asignación", 403, *pep_decision(credential, denied, "/api/v1/realtime/stream"))
    record("PEP-13 applicationName ajeno con credencial propia", 401,
           *pep_decision(credential, allowed, "/api/v1/docente/horarios", app="otra-aplicacion"))
    status, headers, body = pep_decision(credential, allowed, "/api/v1/docente/horarios", environment="prod")
    record("PEP-14 environment distinto (prod)", [204, 403], status, headers, body, allow=status == 204,
           extra="registro informativo: el contrato v0.1.6 no ata el environment a la aplicación")


def run_backend(base, pep_down=False):
    env = L.read_env()
    docente = institutional_token("E2E_DOCENTE_USERNAME", "E2E_DOCENTE_PASSWORD")
    coordinador = institutional_token("E2E_COORDINADOR_USERNAME", "E2E_COORDINADOR_PASSWORD")
    lab = L.password_grant(env, "lab-docente-permitido", "LAB_USER_LAB_DOCENTE_PERMITIDO_PASSWORD")
    if pep_down:
        record("BE-PEPDOWN-01 docente institucional con PEP detenido", 503,
               *backend_get(base, "/api/v1/docente/horarios", docente))
        record("BE-PEPDOWN-02 coordinador institucional con PEP detenido", 503,
               *backend_get(base, "/api/v1/estudiantes", coordinador))
        record("BE-PEPDOWN-03 sin token con PEP detenido", 401, *backend_get(base, "/api/v1/docente/horarios"))
        record("BE-PEPDOWN-04 actuator público con PEP detenido", 200, *backend_get(base, "/actuator/health"))
        return
    record("BE-01 sin token en ruta protegida", 401, *backend_get(base, "/api/v1/docente/horarios"))
    record("BE-02 JWT malformado", 401, *backend_get(base, "/api/v1/docente/horarios", "not.a.jwt"))
    record("BE-03 JWT institucional con firma alterada", 401,
           *backend_get(base, "/api/v1/docente/horarios", tamper(docente)))
    record("BE-04 issuer incorrecto (token del laboratorio)", 401, *backend_get(base, "/api/v1/docente/horarios", lab))
    record("BE-05 Escenario A: docente institucional válido, PDP no reconoce el issuer", 401,
           *backend_get(base, "/api/v1/docente/horarios", docente))
    record("BE-06 Escenario A: coordinador institucional, directorio", 401,
           *backend_get(base, "/api/v1/estudiantes", coordinador))
    record("BE-07 actuator público sin token", 200, *backend_get(base, "/actuator/health"))
    record("BE-08 SSE sin token", 401, *backend_get(base, "/api/v1/realtime/stream",
                                                     extra_headers={"Accept": "text/event-stream"}))
    record("BE-09 SSE docente institucional (PDP rechaza issuer)", 401,
           *backend_get(base, "/api/v1/realtime/stream", docente, {"Accept": "text/event-stream"}))
    record("BE-10 preflight CORS sin credenciales", [200],
           *L.http(L.plain_opener(), "OPTIONS", f"{base}/api/v1/docente/horarios",
                   headers={"Origin": "http://localhost:4200", "Access-Control-Request-Method": "GET",
                            "Access-Control-Request-Headers": "authorization"}))


# --------------------------------------------------------------------------- SEC-BASELINE-002

def denial_row(case, base, path, bearer, layer, expected=403, sse=False, extra_checks=()):
    """403 a través del backend; la capa se comprueba con X-Decision-Id y el log del backend."""
    status, headers, body = sse_open(base, path, bearer) if sse else backend_get(base, path, bearer)
    lower = lower_headers(headers)
    parsed = parse_body(body)
    correlation = lower.get("x-correlation-id") or parsed.get("correlationId")
    time.sleep(0.3)
    app_logged = backend_log_has(correlation, "Handled application exception")
    has_decision = valid_decision_id(lower.get("x-decision-id"))
    if layer == L_PEP:
        checks = [("sin X-Decision-Id: el PEP denegó antes de Spring", not has_decision),
                  ("handler no ejecutado", not app_logged)]
    elif layer == L_ROLE:
        checks = [("X-Decision-Id: el PDP permitió", has_decision), ("use case no ejecutado", not app_logged)]
    else:
        checks = [("X-Decision-Id: el PDP permitió", has_decision),
                  ("GlobalExceptionHandler registró la denegación de Application", app_logged)]
    checks.append(("sin datos académicos", not items(body)))
    checks += json_error_checks(headers, body, "FORBIDDEN", 403) if expected == 403 else []
    checks += list(extra_checks)
    return record(case, expected, status, headers, body, layer=layer, checks=checks)


def direct_pep(case, credential, bearer, path, expect_allow):
    status, headers, body = pep_decision(credential, bearer, path)
    return record(case, 204 if expect_allow else 403, status, headers, body, allow=expect_allow,
                  layer="PEP/PDP directo (precondición)",
                  extra="decisión del PDP para la misma identidad y ruta, sin backend")


def run_e2e(base):
    env = L.read_env()
    credential = env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL"]
    expected = load_expected()[OWN_ID_USUARIO]["expected"]
    docente = lab_token(env, "lab-e2e-docente-fixpag300")
    wrong_role = lab_token(env, "lab-e2e-rol-incorrecto")
    no_pdp = lab_token(env, "lab-e2e-docente-sin-pdp")
    no_id = lab_token(env, "lab-e2e-sin-idusuario")
    wrong_aud = lab_token(env, "lab-e2e-docente-fixpag300", WRONG_AUD_CLIENT)
    institutional = institutional_token("E2E_PAGINACION_300_USERNAME", "E2E_PAGINACION_300_PASSWORD")
    for name, jwt in (("docente", docente), ("rol-incorrecto", wrong_role), ("sin-pdp", no_pdp),
                      ("sin-idUsuario", no_id), ("wrong-aud", wrong_aud), ("institucional", institutional)):
        print(f"claims {name}:", json.dumps(summarize_claims(jwt)))

    # 1. Positivo: JWT válido + PDP + DOCENTE + grupo propio.
    status, headers, body = backend_get(base, "/api/v1/docente/horarios", docente)
    data = items(body)
    record("E2E-01a horarios del docente FIX-PAG-300", 200, status, headers, body, layer=L_ALLOW, count=len(data),
           checks=[("X-Decision-Id válido", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   ("ids == SQL", sorted(h["id"] for h in data) == sorted(h["id"] for h in expected["horarios"])),
                   ("todos del grupo propio", all(h.get("idGrupo") == GROUP_OWN for h in data))])
    status, headers, body = backend_get(base, f"/api/v1/grupos/{GROUP_OWN}/estudiantes", docente)
    data = items(body)
    record("E2E-01b estudiantes del grupo propio FIX-PAG-300", 200, status, headers, body, layer=L_ALLOW,
           count=len(data),
           checks=[("X-Decision-Id válido", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   ("count == matrículas SQL", len(data) == expected["matriculas"])])

    # 2. Ownership: PDP concede EXPRESAMENTE la ruta ajena; la deniega la titularidad académica.
    direct_pep("E2E-02-pre PDP concede sesiones v2 del grupo ajeno", credential, docente,
               f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}", True)
    denial_row("E2E-02a sesiones v2 del grupo ajeno FIX-PAG-1000", base, f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}",
               docente, L_SCOPE)
    direct_pep("E2E-02-pre PDP concede estudiantes del grupo ajeno", credential, docente,
               f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes", True)
    denial_row("E2E-02b estudiantes del grupo ajeno FIX-PAG-1000", base, f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes",
               docente, L_SCOPE)

    # 3. Sin permiso PDP (identidad y rol local correctos).
    direct_pep("E2E-03-pre PDP deniega a la identidad sin asignación", credential, no_pdp, "/api/v1/docente/horarios",
               False)
    denial_row("E2E-03 docente sin asignación PDP", base, "/api/v1/docente/horarios", no_pdp, L_PEP)

    # 4. PDP permite, rol local inadecuado (ESTUDIANTE). En 04b la identidad SÍ es titular del grupo.
    denial_row("E2E-04a rol ESTUDIANTE en horarios de docente", base, "/api/v1/docente/horarios", wrong_role, L_ROLE)
    denial_row("E2E-04b rol ESTUDIANTE, titular de FIX-PAG-1000, sesiones v2", base,
               f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}", wrong_role, L_ROLE)

    # 5-9. Autenticación local; el PEP no se consulta sin autenticación válida.
    for case, jwt, note in (
            ("E2E-05 JWT sin idUsuario", no_id, "única diferencia con E2E-01: falta el claim idUsuario"),
            ("E2E-06 audiencia sin asistencias-api", wrong_aud, "única diferencia: aud sin asistencias-api"),
            ("E2E-07 issuer institucional :8081 (no autorizado en 8083)", institutional, "token válido de otro issuer"),
            ("E2E-08 firma alterada", tamper(docente), "último carácter de la firma modificado"),
            ("E2E-09 sin token", None, "")):
        status, headers, body = backend_get(base, "/api/v1/docente/horarios", jwt)
        record(case, 401, status, headers, body, extra=note, layer=L_JWT if jwt else L_ENTRY,
               checks=[("sin X-Decision-Id", "x-decision-id" not in lower_headers(headers)),
                       ("code UNAUTHORIZED", parse_body(body).get("code") == "UNAUTHORIZED")])

    # 12. Sesiones UTC v2 propias: datos == SQL.
    status, headers, body = backend_get(base, f"/api/v2/sesiones/grupo/{GROUP_OWN}", docente)
    data = items(body)
    sql = {s["id"]: s for s in expected["sesiones"]}
    utc_ok = all(item.get("fechaHoraInicio") == sql[item["sesion"]]["inicio"] + "Z"
                 and item.get("fechaHoraFin") == sql[item["sesion"]]["fin"] + "Z"
                 for item in data if item.get("sesion") in sql and sql[item["sesion"]]["procedenciaTemporal"] == "UTC_V2")
    record("E2E-12 sesiones UTC v2 del grupo propio", 200, status, headers, body, layer=L_ALLOW, count=len(data),
           extra=f"UTC_V2={sum(1 for i in data if i.get('procedenciaTemporal') == 'UTC_V2')}",
           checks=[("X-Decision-Id válido", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   ("ids == SQL", sorted(i.get("sesion") for i in data) == sorted(sql)),
                   ("grupo propio", all(i.get("grupo") == GROUP_OWN for i in data)),
                   ("procedencia == SQL", all(i.get("procedenciaTemporal") == sql[i["sesion"]]["procedenciaTemporal"]
                                               for i in data if i.get("sesion") in sql)),
                   ("instantes UTC_V2 == SQL + Z", utc_ok)])

    # 13. SSE. Contrato: GET /api/v1/realtime/stream?grupoId={UUID}, titularidad al suscribirse. El PEP
    # evalúa getRequestURI() (sin query): el PDP no distingue grupoId y la titularidad es del gateway.
    stream = "/api/v1/realtime/stream"
    status, headers, body = sse_open(base, f"{stream}?grupoId={GROUP_OWN}", docente)
    record("E2E-13a SSE autorizado (grupo propio)", 200, status, headers, body, layer=L_ALLOW,
           checks=[("X-Decision-Id válido", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   ("text/event-stream", "text/event-stream" in lower_headers(headers).get("content-type", ""))])
    # SEC-BASELINE-003: tras FIX-001 el 403 debe ser JSON y no debe abrirse ninguna suscripción. El gauge
    # realtime.subscribers.active (actuator/prometheus, ruta pública) se mide antes y después.
    before = active_subscribers(base)
    denial_row("E2E-13b SSE grupo ajeno (PDP permite la ruta)", base, f"{stream}?grupoId={GROUP_FOREIGN}", docente,
               L_SCOPE, sse=True,
               extra_checks=[("gauge de suscriptores legible", before is not None),
                             ("ninguna suscripción abierta (gauge sin cambios)",
                              before is not None and active_subscribers(base) == before)])
    denial_row("E2E-13c SSE sin asignación PDP", base, f"{stream}?grupoId={GROUP_OWN}", no_pdp, L_PEP, sse=True)
    status, headers, body = sse_open(base, f"{stream}?grupoId={GROUP_OWN}", None)
    record("E2E-13d SSE sin token", 401, status, headers, body, layer=L_ENTRY)
    status, headers, body = sse_open(base, stream, docente)
    record("E2E-13e SSE sin grupoId (contrato: obligatorio)", 400, status, headers, body,
           layer="Controller (validación de contrato)",
           checks=[("X-Decision-Id: el PDP permitió la ruta", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   *json_error_checks(headers, body, "VALIDATION_ERROR", 400)])
    status, headers, body = sse_open(base, f"{stream}?grupoId=no-es-uuid", docente)
    record("E2E-13f SSE grupoId no UUID", 400, status, headers, body, layer="Controller (validación de contrato)",
           checks=[("X-Decision-Id: el PDP permitió la ruta", valid_decision_id(lower_headers(headers).get("x-decision-id"))),
                   *json_error_checks(headers, body, "INVALID_REQUEST", 400)])
    after = active_subscribers(base)
    record("E2E-13g sin suscripciones residuales tras la matriz SSE", 200, 200, {}, b"", layer="Observabilidad",
           extra=f"realtime_subscribers_active antes={before} después={after}",
           checks=[("gauge legible", after is not None), ("gauge == valor inicial", after == before)])

    # Informativos de la limitación de rutas exactas (v0.1.6) y de rutas públicas.
    denial_row("E2E-14 UUID del grupo propio en mayúsculas (ruta no registrada)", base,
               f"/api/v2/sesiones/grupo/{GROUP_OWN.upper()}", docente, L_PEP)
    record("E2E-15 actuator público", 200, *backend_get(base, "/actuator/health"), layer=L_PUBLIC)


def run_e2e_pep_down(base):
    env = L.read_env()
    docente = lab_token(env, "lab-e2e-docente-fixpag300")
    status, headers, body = backend_get(base, "/api/v1/docente/horarios", docente)
    record("E2E-10a PEP inaccesible: horarios", 503, status, headers, body, layer=L_UNAVAILABLE,
           checks=[("code ACCESS_DECISION_UNAVAILABLE", parse_body(body).get("code") == "ACCESS_DECISION_UNAVAILABLE"),
                   ("sin datos", not items(body))])
    status, headers, body = sse_open(base, f"/api/v1/realtime/stream?grupoId={GROUP_OWN}", docente)
    record("E2E-10b PEP inaccesible: SSE", 503, status, headers, body, layer=L_UNAVAILABLE)
    record("E2E-10c PEP inaccesible: sin token", 401, *backend_get(base, "/api/v1/docente/horarios"), layer=L_ENTRY)
    record("E2E-10d PEP inaccesible: actuator", 200, *backend_get(base, "/actuator/health"), layer=L_PUBLIC)


def run_stable(base):
    """SEC-BASELINE-003: el MISMO jar con el flag PEP OFF y la identidad institucional (8081) conserva el
    comportamiento estable: sin consulta al PEP (nunca X-Decision-Id) y con los status/códigos de develop."""
    env = L.read_env()
    docente = institutional_token("E2E_PAGINACION_300_USERNAME", "E2E_PAGINACION_300_PASSWORD")
    lab = lab_token(env, "lab-e2e-docente-fixpag300")
    print("claims institucional:", json.dumps(summarize_claims(docente)))
    layer = "Backend estable (PEP OFF)"

    def no_decision(headers):
        return ("sin X-Decision-Id (PEP no consultado)", "x-decision-id" not in lower_headers(headers))

    status, headers, body = backend_get(base, "/api/v1/docente/horarios", docente)
    record("ST-01 horarios docente institucional", 200, status, headers, body, layer=layer, count=len(items(body)),
           checks=[no_decision(headers), ("al menos un horario", len(items(body)) >= 1)])
    status, headers, body = backend_get(base, f"/api/v1/grupos/{GROUP_OWN}/estudiantes", docente)
    record("ST-02 estudiantes del grupo propio", 200, status, headers, body, layer=layer, count=len(items(body)),
           checks=[no_decision(headers)])
    status, headers, body = backend_get(base, f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes", docente)
    record("ST-03 estudiantes del grupo ajeno", 403, status, headers, body, layer=layer,
           checks=[no_decision(headers), ("sin datos", not items(body)),
                   *json_error_checks(headers, body, "FORBIDDEN", 403)])
    stream = "/api/v1/realtime/stream"
    status, headers, body = sse_open(base, f"{stream}?grupoId={GROUP_OWN}", docente)
    record("ST-04 SSE grupo propio", 200, status, headers, body, layer=layer,
           checks=[no_decision(headers),
                   ("text/event-stream", "text/event-stream" in lower_headers(headers).get("content-type", ""))])
    status, headers, body = sse_open(base, f"{stream}?grupoId={GROUP_FOREIGN}", docente)
    record("ST-05 SSE grupo ajeno", 403, status, headers, body, layer=layer,
           checks=[no_decision(headers), *json_error_checks(headers, body, "FORBIDDEN", 403)])
    status, headers, body = sse_open(base, stream, docente)
    record("ST-06 SSE sin grupoId", 400, status, headers, body, layer=layer,
           checks=[*json_error_checks(headers, body, "VALIDATION_ERROR", 400)])
    status, headers, body = backend_get(base, "/api/v1/docente/horarios", lab)
    record("ST-07 token del laboratorio (issuer 19090) rechazado", 401, status, headers, body, layer=layer,
           checks=[no_decision(headers)])
    record("ST-08 sin token", 401, *backend_get(base, "/api/v1/docente/horarios"), layer=layer)
    record("ST-09 actuator público", 200, *backend_get(base, "/actuator/health"), layer=L_PUBLIC)


def run_e2e_pdp_down(base):
    """SEC-BASELINE-003: PEP arriba y PDP detenido (docker stop asistencias-security-lab-pdp-1). El PEP no
    puede decidir; el backend debe cerrar (503) sin datos y sin propagar decisiones."""
    env = L.read_env()
    credential = env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL"]
    docente = lab_token(env, "lab-e2e-docente-fixpag300")
    status, headers, body = pep_decision(credential, docente, "/api/v1/docente/horarios")
    record("E2E-16-pre PEP con PDP detenido (directo)", [500, 502, 503, 504], status, headers, body,
           layer="PEP directo (precondición)", checks=[("sin X-Decision-Id", "x-decision-id" not in lower_headers(headers))])
    status, headers, body = backend_get(base, "/api/v1/docente/horarios", docente)
    record("E2E-16a PDP inaccesible: horarios", 503, status, headers, body, layer=L_UNAVAILABLE,
           checks=[("code ACCESS_DECISION_UNAVAILABLE", parse_body(body).get("code") == "ACCESS_DECISION_UNAVAILABLE"),
                   ("sin datos", not items(body))])
    status, headers, body = sse_open(base, f"/api/v1/realtime/stream?grupoId={GROUP_OWN}", docente)
    record("E2E-16b PDP inaccesible: SSE", 503, status, headers, body, layer=L_UNAVAILABLE,
           checks=[("code ACCESS_DECISION_UNAVAILABLE", parse_body(body).get("code") == "ACCESS_DECISION_UNAVAILABLE")])
    record("E2E-16c PDP inaccesible: sin token", 401, *backend_get(base, "/api/v1/docente/horarios"), layer=L_ENTRY)
    record("E2E-16d PDP inaccesible: actuator", 200, *backend_get(base, "/actuator/health"), layer=L_PUBLIC)


# Rutas que pep_fault_stub.py asocia a cada respuesta anómala.
FAULTS = [
    ("E2E-11a PEP 200 vacío sin decisión (comportamiento PEP-03)", "/api/v1/docente/horarios"),
    ("E2E-11b PEP 204 sin X-Decision-Id", "/api/v1/realtime/stream"),
    ("E2E-11c PEP 204 con X-Decision-Id inválido", f"/api/v1/grupos/{GROUP_OWN}/estudiantes"),
    ("E2E-11d PEP 500", f"/api/v2/sesiones/grupo/{GROUP_OWN}"),
    ("E2E-11e PEP 302 redirect", f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes"),
    ("E2E-11f PEP 200 con X-Decision-Id válido (2xx distinto de 204)", f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}"),
    ("E2E-11g PEP excede el timeout", "/api/v1/docente/asignaturas"),
]


def run_e2e_fault(base):
    env = L.read_env()
    docente = lab_token(env, "lab-e2e-docente-fixpag300")
    for case, path in FAULTS:
        status, headers, body = (sse_open(base, f"{path}?grupoId={GROUP_OWN}", docente) if path.endswith("/stream")
                                 else backend_get(base, path, docente))
        record(case, 503, status, headers, body, layer=L_UNAVAILABLE,
               checks=[("code ACCESS_DECISION_UNAVAILABLE",
                        parse_body(body).get("code") == "ACCESS_DECISION_UNAVAILABLE"),
                       ("sin datos", not items(body)),
                       ("sin X-Decision-Id propagado", "x-decision-id" not in lower_headers(headers))])


# --------------------------------------------------------------------------- selftest

def selftest():
    import unittest

    class VerdictTest(unittest.TestCase):
        def test_allow_requires_204(self):
            self.assertEqual(verdict_for("X-1 a", [200, 204], 200, allow=True, decision_id="d-1")[0], FAIL)

        def test_allow_requires_decision_id(self):
            self.assertEqual(verdict_for("X-1 a", 204, 204, allow=True, decision_id=None)[0], FAIL)
            self.assertEqual(verdict_for("X-1 a", 204, 204, allow=True, decision_id="bad id!")[0], FAIL)
            self.assertEqual(verdict_for("X-1 a", 204, 204, allow=True, decision_id="0e1e296a-504c")[0], PASS)

        def test_known_upstream_is_never_pass(self):
            self.assertEqual(verdict_for("PEP-03 sin Bearer", 401, 200)[0], KNOWN)
            self.assertEqual(verdict_for("PEP-03 sin Bearer", 401, 401)[0], PASS)

        def test_extra_checks(self):
            self.assertEqual(verdict_for("E2E-01 x", 200, 200, checks=[("c", False)])[0], FAIL)

        def test_exit_codes(self):
            self.assertEqual(exit_code([{"verdict": PASS}]), 0)
            self.assertEqual(exit_code([{"verdict": PASS}, {"verdict": KNOWN}]), 2)
            self.assertEqual(exit_code([{"verdict": KNOWN}, {"verdict": FAIL}]), 1)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(VerdictTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


def exit_code(rows):
    if any(r["verdict"] == FAIL for r in rows):
        return 1
    if any(r["verdict"] == KNOWN for r in rows):
        return 2
    return 0


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "pep"
    target = sys.argv[2] if len(sys.argv) > 2 else "http://127.0.0.1:8083"
    modes = {"pep": run_pep, "backend": lambda: run_backend(target),
             "backend-pep-down": lambda: run_backend(target, pep_down=True),
             "e2e": lambda: run_e2e(target), "e2e-pep-down": lambda: run_e2e_pep_down(target),
             "e2e-fault": lambda: run_e2e_fault(target), "stable": lambda: run_stable(target),
             "e2e-pdp-down": lambda: run_e2e_pdp_down(target)}
    if mode == "selftest":
        sys.exit(selftest())
    if mode not in modes:
        raise SystemExit(f"modo desconocido: {mode}")
    modes[mode]()
    own = sum(1 for r in results if r["verdict"] == FAIL)
    known = sum(1 for r in results if r["verdict"] == KNOWN)
    print(f"TOTAL={len(results)} PASS={len(results) - own - known} FAIL={own} KNOWN_UPSTREAM_FAIL={known}")
    sys.exit(exit_code(results))
