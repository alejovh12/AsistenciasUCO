"""SEC-BASELINE-001 — matriz HTTP real contra el laboratorio (solo GET / decisiones, sin escrituras).

Uso:  python -I lab_matrix.py pep            -> decisiones reales PEP/PDP (sin backend)
      python -I lab_matrix.py backend [url]  -> backend experimental con PEP ON (por defecto :8083)
      python -I lab_matrix.py backend-pep-down [url]

Los tokens se obtienen en memoria y nunca se imprimen ni se escriben. El env institucional se lee
en solo lectura desde infra/keycloak/.env del checkout estable (no se copia al laboratorio).
"""
import base64
import json
import os
import sys
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402

INSTITUTIONAL_ENV = os.environ.get(
    "ASISTENCIAS_INSTITUTIONAL_ENV",
    r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\keycloak\.env")
INSTITUTIONAL_KC = "http://127.0.0.1:8081"

results = []


def read_dotenv(path):
    values = {}
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip().strip('"')
    return values


def institutional_token(role):
    env = read_dotenv(INSTITUTIONAL_ENV)
    realm = env.get("KC_REALM", "asistencias-uco")
    client = env.get("KC_FRONTEND_CLIENT_ID", "asistencias-uco-frontend")
    status, _, raw = L.http(L.plain_opener(), "POST",
                            f"{INSTITUTIONAL_KC}/realms/{realm}/protocol/openid-connect/token",
                            {"grant_type": "password", "client_id": client,
                             "username": env[f"E2E_{role}_USERNAME"], "password": env[f"E2E_{role}_PASSWORD"],
                             "scope": "openid"}, form=True)
    if status != 200:
        raise SystemExit(f"institutional token {role}: HTTP {status}")
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


def record(case, expected, status, headers, body, extra=""):
    try:
        parsed = json.loads(body or b"{}")
        code = parsed.get("code") or parsed.get("title")
    except ValueError:
        code = None
    lower = {k.lower(): v for k, v in headers.items()}
    verdict = "PASS" if str(status) in [str(e) for e in (expected if isinstance(expected, (list, tuple)) else [expected])] else "FAIL"
    row = {"case": case, "expected": expected, "status": status, "code": code,
           "correlationId": lower.get("x-correlation-id"), "decisionId": lower.get("x-decision-id"),
           "verdict": verdict, "note": extra}
    results.append(row)
    print(json.dumps(row, ensure_ascii=False))


def pep_decision(credential, bearer, path, method="GET", app="asistencias-uco-lab", environment="local"):
    headers = {"X-Correlation-Id": f"sec-lab-{uuid.uuid4()}", "Accept": "application/json"}
    if credential is not None:
        headers["X-Application-Credential"] = credential
    if bearer is not None:
        headers["Authorization"] = f"Bearer {bearer}"
    return L.http(L.plain_opener(), "POST", f"{L.PEP}/internal/v1/embedded-access-decisions",
                  {"applicationName": app, "environment": environment, "path": path, "method": method},
                  headers=headers)


def run_pep():
    env = L.read_env()
    credential = env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL"]
    allowed = L.password_grant(env, "lab-docente-permitido", "LAB_USER_LAB_DOCENTE_PERMITIDO_PASSWORD")
    denied = L.password_grant(env, "lab-docente-sin-permiso", "LAB_USER_LAB_DOCENTE_SIN_PERMISO_PASSWORD")
    print("lab token claims:", json.dumps(summarize_claims(allowed)))
    institutional = institutional_token("DOCENTE")
    print("institutional token claims:", json.dumps(summarize_claims(institutional)))

    record("PEP-01 sin credencial de aplicación", 401, *pep_decision(None, allowed, "/api/v1/docente/horarios"))
    record("PEP-02 credencial de aplicación incorrecta", 401,
           *pep_decision("x" * len(credential), allowed, "/api/v1/docente/horarios"))
    record("PEP-03 sin Bearer", 401, *pep_decision(credential, None, "/api/v1/docente/horarios"))
    record("PEP-04 Bearer con firma alterada", 401, *pep_decision(credential, tamper(allowed), "/api/v1/docente/horarios"))
    record("PEP-05 Escenario A: JWT institucional (issuer :8081)", 401,
           *pep_decision(credential, institutional, "/api/v1/docente/horarios"))
    record("PEP-06 lab token con rol PDP sobre recurso concedido", [200, 204],
           *pep_decision(credential, allowed, "/api/v1/docente/horarios"))
    record("PEP-07 lab token sin asignación PDP", 403, *pep_decision(credential, denied, "/api/v1/docente/horarios"))
    record("PEP-08 recurso registrado no concedido al rol", 403,
           *pep_decision(credential, allowed, "/api/v1/estudiantes"))
    record("PEP-09 ruta no registrada", 403, *pep_decision(credential, allowed, "/api/v1/admin/sedes"))
    record("PEP-10 método distinto al concedido", 403,
           *pep_decision(credential, allowed, "/api/v1/docente/horarios", method="POST"))
    record("PEP-11 SSE concedido", [200, 204], *pep_decision(credential, allowed, "/api/v1/realtime/stream"))
    record("PEP-12 SSE sin asignación", 403, *pep_decision(credential, denied, "/api/v1/realtime/stream"))
    record("PEP-13 applicationName ajeno con credencial propia", 401,
           *pep_decision(credential, allowed, "/api/v1/docente/horarios", app="otra-aplicacion"))
    record("PEP-14 environment distinto (prod)", [200, 204, 403],
           *pep_decision(credential, allowed, "/api/v1/docente/horarios", environment="prod"),
           extra="registro informativo: el contrato v0.1.6 no ata el environment a la aplicación")


def backend_get(base, path, bearer=None, extra_headers=None):
    headers = {"X-Correlation-Id": str(uuid.uuid4()), "Accept": "application/json"}
    if bearer is not None:
        headers["Authorization"] = f"Bearer {bearer}"
    headers.update(extra_headers or {})
    return L.http(L.plain_opener(), "GET", f"{base}{path}", headers=headers)


def run_backend(base, pep_down=False):
    env = L.read_env()
    docente = institutional_token("DOCENTE")
    coordinador = institutional_token("COORDINADOR")
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


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "pep"
    target = sys.argv[2] if len(sys.argv) > 2 else "http://127.0.0.1:8083"
    if mode == "pep":
        run_pep()
    elif mode == "backend":
        run_backend(target)
    elif mode == "backend-pep-down":
        run_backend(target, pep_down=True)
    failed = [r for r in results if r["verdict"] != "PASS"]
    print(f"TOTAL={len(results)} PASS={len(results) - len(failed)} FAIL={len(failed)}")
