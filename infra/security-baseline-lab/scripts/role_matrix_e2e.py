"""SEC-BASELINE-003 — matriz académica por roles con proveedor REAL (Keycloak 19090 + PEP/PDP v0.1.6) y
backend 8083 sobre la DB E2E AISLADA (login de mínimo privilegio, nunca sa).

Uso: python -I role_matrix_e2e.py provision        -> Keycloak/PDP del laboratorio (idempotente)
     python -I role_matrix_e2e.py run [url]        -> RBAC por rol, paginación 300/1000/5000, escrituras,
                                                      reclamos, descargas, DELETE, CORS/CSRF
     python -I role_matrix_e2e.py revocation [url] -> sesiones revocadas / usuario deshabilitado
                                                      (solo lab-e2e-revocable, que se restablece al final)
     python -I role_matrix_e2e.py selftest         -> pruebas del arnés (sin red)

Efectos declarados:
  provision  Keycloak 19090 (realm security-baseline): client roles ADMINISTRADOR/DECANO/COORDINADOR en
             asistencias-api y su scope mapping en asistencias-lab-e2e; usuarios lab-e2e-* nuevos (contraseñas
             aleatorias en lab.env). PDP: recursos EXACTOS, rol MATRIZ_ROLES_E2E_LAB y asignaciones. No
             modifica recursos, roles ni asignaciones existentes. Exige verify-role-identities.ps1 < 24 h.
  run        Escrituras SOLO tras assert-backend-isolated.ps1 (PID del backend con sesiones únicamente en
             el SQL E2E aislado). Registra en el PDP recursos exactos de los ids creados (plantillas -> 500).
  revocation Cierra las sesiones y deshabilita/rehabilita SOLO lab-e2e-revocable.
Nunca imprime contraseñas, secretos ni tokens. Exit: 0 todo PASS; 1 algún FAIL; 2 solo límites conocidos.
"""
import contextlib
import datetime as dt
import io
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lab_pdp as L  # noqa: E402
import lab_matrix as M  # noqa: E402

VERIFICATION = os.path.join(os.path.dirname(L.ENV_PATH), "role-identity-verification.json")
EVIDENCE = os.path.join(os.path.dirname(L.ENV_PATH), "role-matrix-provisioning-evidence.json")
APPLICATION = "asistencias-uco-lab"
API_CLIENT = "asistencias-api"
E2E_CLIENT = "asistencias-lab-e2e"
ALL_ROLES = ["ADMINISTRADOR", "DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE"]
PDP_ROLE = "MATRIZ_ROLES_E2E_LAB"
ISSUER = f"{L.KEYCLOAK}/realms/{L.REALM}"
FIX = {300: "de000005-0000-4000-8000-000000000001", 1000: "de000005-0000-4000-8000-000000000002",
       5000: "de000005-0000-4000-8000-000000000003"}
ORIGIN_OK = "http://localhost:4200"
ORIGIN_BAD = "https://evil.example"

# Límites de diseño conocidos (nunca PASS; exit 2). Clave = prefijo del caso.
DESIGN_LIMITS = {
    "REV-02": "JWT validado localmente sin introspección: la revocación de sesiones no invalida el access token hasta exp",
    "REV-04": "JWT validado localmente sin introspección: deshabilitar al usuario no invalida el access token vigente",
}
# Defectos AS-IS de AsistenciasUCO ya diagnosticados (nunca PASS; exit 2). Clave = prefijo exacto del caso.
KNOWN_DEFECTS = {
    "DL-03 ": "SECURITY_FINDING: el reporte Excel de asistencia no valida titularidad del docente (IDOR)",
    "RB-admin GET /api/v1/decano/coordinadores": "política documentada DECANO o ADMINISTRADOR; el caso de uso exige la "
                                                 "facultad del decano autenticado (fail-closed)",
}


def load_verification():
    if not os.path.exists(VERIFICATION):
        raise SystemExit("BLOCKED: falta verify-role-identities.ps1")
    with open(VERIFICATION, encoding="utf-8-sig") as handle:
        evidence = json.load(handle)
    when = dt.datetime.fromisoformat(evidence["verifiedAt"].replace("Z", "+00:00")[:26] + "+00:00")
    if dt.datetime.now(dt.timezone.utc) - when > dt.timedelta(hours=24):
        raise SystemExit("BLOCKED: verificación de identidades con más de 24 h; repetirla")
    return evidence


def identity(evidence, username):
    found = next(i for i in evidence["identities"] if i["username"] == username)
    if not found["verified"]:
        raise SystemExit(f"BLOCKED: {username} no está verificado en SQL")
    return found


# --------------------------------------------------------------------------- recursos PDP

def static_routes(ctx):
    g001 = ctx["grupoDocenteSeed"]
    est = ctx["estudianteSeedId"]
    gets = ["/api/v1/admin/sedes", "/api/v1/admin/decanos", "/api/v1/decano/coordinadores",
            "/api/v1/coordinador/docentes", "/api/v1/coordinador/periodos-academicos",
            "/api/v1/docente/horarios", "/api/v1/estudiante/horarios", "/api/v1/docentes", "/api/v1/estudiantes",
            "/api/v1/grupos", "/api/v1/usuarios/perfil", "/api/v1/tipos-identificacion", "/api/v1/realtime/status",
            "/api/v1/realtime/stream", f"/api/v1/grupos/{g001}/estudiantes", f"/api/v1/grupos/{g001}/asistencias",
            f"/api/v1/grupos/{g001}/reportes/asistencia-excel", f"/api/v2/sesiones/grupo/{g001}",
            f"/api/v1/sesiones/grupo/{g001}"] + [f"/api/v1/grupos/{g}/estudiantes" for g in FIX.values()]
    routes = [("GET", p) for p in gets]
    routes += [("POST", "/api/v2/sesiones"), ("POST", "/api/v1/asistencias/lote"),
               ("POST", "/api/v1/asistencias/revisiones"), ("POST", "/api/v1/archivos/subir"),
               ("DELETE", f"/api/v1/grupos/{g001}/estudiantes/{est}")]
    # NO concedida a propósito (PDP no-grant con rol local válido): GET /api/v1/admin/auditoria.
    return routes


class Pdp:
    def __init__(self, env):
        opener, jar, _, _, _ = L.bff_session(env, "lab-pdp-admin", "LAB_USER_LAB_PDP_ADMIN_PASSWORD")
        self.opener, self.headers = opener, L.xsrf_headers(jar)
        apps = self.call("GET", "/api/v1/applications?size=100")["data"]["content"]
        self.app_id = next(a["id"] for a in apps if a["name"] == APPLICATION)
        self.role_id = None

    def call(self, method, path, body=None, ok=(200, 201)):
        status, _, raw = L.http(self.opener, method, f"{L.PDP}{path}", body, headers=self.headers)
        parsed = json.loads(raw) if raw else {}
        if status not in ok:
            raise SystemExit(f"PDP {method} {path.split('?')[0]}: HTTP {status} {parsed.get('code')}")
        return parsed

    def ensure_role(self):
        roles = self.call("GET", f"/api/v1/applications/{self.app_id}/security/roles?size=100")["data"]["content"]
        role = next((r for r in roles if r["name"] == PDP_ROLE), None)
        self.role_id = role["id"] if role else self.call("POST", "/api/v1/roles", {
            "name": PDP_ROLE, "scope": "APPLICATION", "applicationId": self.app_id})["data"]["id"]
        return self.role_id

    def grant(self, routes):
        """Crea los recursos exactos que falten y los concede al rol de la matriz. Devuelve {método ruta: id}."""
        existing = self.call("GET", f"/api/v1/applications/{self.app_id}/resources").get("data") or []
        by_key = {(r["path"], r["method"]): r["id"] for r in existing}
        created = []
        for method, path in routes:
            if (path, method) not in by_key:
                by_key[(path, method)] = self.call("POST", f"/api/v1/applications/{self.app_id}/resources",
                                                   {"path": path, "method": method})["data"]["id"]
                created.append(f"{method} {path}")
            self.call("POST", f"/api/v1/roles/{self.role_id}/resources",
                      {"roleId": self.role_id, "resourceId": by_key[(path, method)]}, ok=(200, 201, 409))
        return {f"{m} {p}": by_key[(p, m)] for m, p in routes}, created


# --------------------------------------------------------------------------- provision

def provision():
    import provision_shared_identity_e2e as P
    evidence = load_verification()
    verified = {i["idUsuario"].lower(): i for i in evidence["identities"] if i["verified"]}
    env = L.read_env()
    admin = P.Admin(env)
    api_client, _ = P.ensure_api_client(admin)
    have = {r["name"] for r in admin.call("GET", f"/clients/{api_client['id']}/roles")}
    for role in ALL_ROLES:
        if role not in have:
            admin.call("POST", f"/clients/{api_client['id']}/roles", {"name": role, "description": f"Laboratorio: {role}"})
    api_roles = {r["name"]: r for r in admin.call("GET", f"/clients/{api_client['id']}/roles")}
    token_client = admin.client(E2E_CLIENT)
    if token_client is None:
        raise SystemExit("BLOCKED: falta asistencias-lab-e2e (ejecute provision_shared_identity_e2e.py)")
    mapped = {r["name"] for r in admin.call("GET", f"/clients/{token_client['id']}/scope-mappings/clients/{api_client['id']}")}
    missing = [api_roles[r] for r in ALL_ROLES if r not in mapped]
    if missing:
        admin.call("POST", f"/clients/{token_client['id']}/scope-mappings/clients/{api_client['id']}", missing)
    print(f"client {API_CLIENT} roles={sorted(api_roles)}; {E2E_CLIENT} role scope={ALL_ROLES}")

    kc_ids, password_keys, users = {}, {}, []
    for item in evidence["identities"]:
        username = item["username"]
        if username == "lab-e2e-sin-rol":
            user_id, key = L.ensure_lab_user(env, admin.headers["Authorization"].split(" ", 1)[1], username,
                                             "LabE2E", "sin-rol")
            rep = admin.call("GET", f"/users/{user_id}")
            rep["attributes"] = {**(rep.get("attributes") or {}), "idUsuario": [item["idUsuario"]]}
            rep.update({"emailVerified": True, "requiredActions": [], "enabled": True})
            admin.call("PUT", f"/users/{user_id}", rep)
            current = admin.call("GET", f"/users/{user_id}/role-mappings/clients/{api_client['id']}")
            if current:
                admin.call("DELETE", f"/users/{user_id}/role-mappings/clients/{api_client['id']}", current)
            print(f"user {username}: kc_id={user_id} idUsuario={item['idUsuario']} asistencias-api role=<ninguno>")
        else:
            rep = admin.call("GET", f"/users?username={username}&exact=true")
            if rep and not rep[0].get("enabled", True):
                rep[0]["enabled"] = True
                admin.call("PUT", f"/users/{rep[0]['id']}", rep[0])
            user_id, key = P.ensure_user(admin, env, username, item["idUsuario"], item["role"], verified,
                                         api_client, api_roles)
        kc_ids[username], password_keys[username] = user_id, key
        users.append(item)

    for username, key in password_keys.items():
        _, _, status, _, _ = L.bff_session(env, username, key)
        print(f"pdp login {username}: BFF session HTTP {status}")
    pdp = Pdp(env)
    role_id = pdp.ensure_role()
    resources, created = pdp.grant(static_routes(evidence["context"]))
    print(f"pdp role {PDP_ROLE}: {role_id}; recursos concedidos={len(resources)} creados={len(created)}")
    pdp_users = pdp.call("GET", "/api/v1/users?size=200")["data"]
    assigned = {a.get("userId") for a in (pdp.call("GET", f"/api/v1/roles/{role_id}/assignments?size=100")
                                          .get("data") or {}).get("content", [])}
    pdp_ids = {}
    for username in password_keys:
        pdp_user = next(u for u in pdp_users if u["email"].startswith(f"{username}@"))
        pdp_ids[username] = pdp_user["id"]
        if pdp_user["id"] not in assigned:
            pdp.call("POST", f"/api/v1/roles/{role_id}/assignments",
                     {"roleId": role_id, "userId": pdp_user["id"], "applicationId": pdp.app_id})
        print(f"pdp user {username}: assigned {PDP_ROLE}")

    claims = verify_claims(env, users, kc_ids)
    with open(EVIDENCE, "w", encoding="utf-8") as handle:
        json.dump({"workItem": "SEC-BASELINE-003", "provisionedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
                   "sqlVerification": evidence["verifiedAt"],
                   "keycloak": {"issuer": ISSUER, "users": {u["username"]: {"keycloakId": kc_ids[u["username"]],
                                                                             "idUsuario": u["idUsuario"],
                                                                             "role": u["role"]} for u in users}},
                   "pdp": {"applicationId": pdp.app_id, "roleId": role_id, "resources": resources,
                           "createdResources": created, "pdpUserIds": pdp_ids},
                   "claims": claims}, handle, indent=2)
    print(f"evidence (sin secretos): {EVIDENCE}")
    if any(r["verdict"] != "PASS" for r in claims):
        raise SystemExit(1)


def verify_claims(env, users, kc_ids):
    import base64
    _, _, raw = L.http(L.plain_opener(), "GET", f"{ISSUER}/protocol/openid-connect/certs")
    kids = {k["kid"] for k in json.loads(raw)["keys"] if k.get("use") == "sig"}
    rows = []
    for item in users:
        username = item["username"]
        jwt = M.lab_token(env, username)
        header = json.loads(base64.urlsafe_b64decode(jwt.split(".")[0] + "=" * (-len(jwt.split(".")[0]) % 4)))
        claims = L.jwt_claims(jwt)
        aud = claims.get("aud") if isinstance(claims.get("aud"), list) else [claims.get("aud")]
        roles = ((claims.get("resource_access") or {}).get(API_CLIENT) or {}).get("roles")
        expected_roles = None if username == "lab-e2e-sin-rol" else [item["role"]]
        row = {"user": username, "alg": header.get("alg"), "kidInJwks": header.get("kid") in kids,
               "iss": claims.get("iss"), "aud": sorted(a for a in aud if a), "roles": roles,
               "idUsuario": claims.get("idUsuario"), "subIsKeycloakId": claims.get("sub") == kc_ids[username],
               "subDiffersFromIdUsuario": claims.get("sub") != claims.get("idUsuario"),
               "lifetimeSeconds": claims.get("exp", 0) - claims.get("iat", 0)}
        checks = [row["alg"] == "RS256", row["kidInJwks"], row["iss"] == ISSUER,
                  {"security-baseline-pdp", API_CLIENT} <= set(row["aud"]), (roles or None) == expected_roles,
                  row["idUsuario"] == item["idUsuario"], row["subIsKeycloakId"], row["subDiffersFromIdUsuario"],
                  0 < row["lifetimeSeconds"] <= 300]
        row["verdict"] = "PASS" if all(checks) else "FAIL"
        rows.append(row)
        print("claims:", json.dumps(row))
    return rows


# --------------------------------------------------------------------------- HTTP

def call(base, method, path, bearer=None, body=None, headers=None):
    h = {"X-Correlation-Id": str(uuid.uuid4()), "Accept": "application/json"}
    if bearer is not None:
        h["Authorization"] = f"Bearer {bearer}"
    h.update(headers or {})
    return L.http(L.plain_opener(), method, f"{base}{path}", body, headers=h)


def multipart(base, path, bearer, field, filename, content_type, content):
    boundary = uuid.uuid4().hex
    data = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{filename}\"\r\n"
            f"Content-Type: {content_type}\r\n\r\n").encode() + content + f"\r\n--{boundary}--\r\n".encode()
    request = urllib.request.Request(f"{base}{path}", data=data, method="POST", headers={
        "Authorization": f"Bearer {bearer}", "Accept": "application/json", "X-Correlation-Id": str(uuid.uuid4()),
        "Content-Type": f"multipart/form-data; boundary={boundary}"})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.status, dict(response.headers), response.read()
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read()


def lower(headers):
    return M.lower_headers(headers)


def has_decision(headers):
    return M.valid_decision_id(lower(headers).get("x-decision-id"))


def data_of(body):
    parsed = M.parse_body(body)
    return parsed.get("datos") if isinstance(parsed, dict) else None


def row(case, expected, response, layer=None, checks=(), count=None, extra=""):
    status, headers, body = response
    with contextlib.redirect_stdout(io.StringIO()):
        result = M.record(case, expected, status, headers, body, layer=layer, checks=checks, count=count, extra=extra)
    key = case.split(" ", 1)[0]
    defect = next((k for k in KNOWN_DEFECTS if case.startswith(k)), None)
    if result["verdict"] == M.FAIL and key in DESIGN_LIMITS:
        result["verdict"] = "KNOWN_DESIGN_LIMIT"
        result["note"] = (result["note"] + "; " if result["note"] else "") + DESIGN_LIMITS[key]
    elif result["verdict"] == M.FAIL and defect:
        result["verdict"] = "KNOWN_DEFECT"
        result["note"] = (result["note"] + "; " if result["note"] else "") + KNOWN_DEFECTS[defect]
    print(json.dumps(result, ensure_ascii=False))
    return result


def readonly_query(sql, **params):
    """SELECT con asistencias_e2e_readonly sobre la DB aislada (la API no expone algunos ids)."""
    out = subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                          os.path.join(HERE, "e2e-readonly-query.ps1"), "-Query", sql,
                          "-ParamsJson", json.dumps(params)], capture_output=True, text=True, timeout=120)
    if out.returncode != 0:
        raise SystemExit(f"readonly query falló: {out.stderr.strip()[:300]}")
    text = out.stdout.strip()
    parsed = json.loads(text) if text else []
    return parsed if isinstance(parsed, list) else [parsed]


def assert_isolated(port):
    out = subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                          os.path.join(HERE, "assert-backend-isolated.ps1"), "-Port", str(port)],
                         capture_output=True, text=True, timeout=120)
    print("isolation guard:", out.stdout.strip())
    return out.returncode == 0, out.stdout.strip()


# --------------------------------------------------------------------------- run

def expected_rbac(ctx):
    g001 = ctx["grupoDocenteSeed"]
    everyone = {"admin", "decano", "coord", "docente", "estudiante", "sinrol", "doc1000"}
    # (ruta, roles que superan RBAC, status esperado para esos roles). 501 = el controlador declara
    # FeatureUnavailableException en el AS-IS (verificado en código): la autorización pasa y no hay datos.
    return [
        ("/api/v1/admin/decanos", {"admin"}, 200),
        ("/api/v1/admin/sedes", {"admin"}, 501),
        ("/api/v1/decano/coordinadores", {"decano", "admin"}, 200),
        ("/api/v1/coordinador/periodos-academicos", {"coord", "admin"}, 200),
        ("/api/v1/coordinador/docentes", {"coord", "admin"}, 501),
        ("/api/v1/docente/horarios", {"docente", "doc1000"}, 200),
        ("/api/v1/estudiante/horarios", {"estudiante"}, 200),
        ("/api/v1/docentes", {"coord", "admin"}, 200),
        ("/api/v1/estudiantes?size=5", {"coord", "admin"}, 200),
        ("/api/v1/grupos", {"coord", "admin"}, 200),
        ("/api/v1/usuarios/perfil", everyone, 501),
        ("/api/v1/tipos-identificacion", everyone, 200),
        ("/api/v1/realtime/status", everyone, 200),
        (f"/api/v1/grupos/{g001}/estudiantes", {"docente", "coord", "admin"}, 200),  # doc1000: rol sí, titularidad no
    ]


def run(base):
    evidence = load_verification()
    ctx = evidence["context"]
    port = int(base.rsplit(":", 1)[1].split("/")[0])
    isolated, guard = assert_isolated(port)
    if not isolated:
        raise SystemExit("BLOCKED: el backend no está aislado; no se ejecuta ninguna escritura ni la matriz")
    env = L.read_env()
    users = {"admin": "lab-e2e-administrador", "decano": "lab-e2e-decano", "coord": "lab-e2e-coordinador",
             "docente": "lab-e2e-docente-seed", "estudiante": "lab-e2e-estudiante", "sinrol": "lab-e2e-sin-rol",
             "doc1000": "lab-e2e-docente-fixpag1000", "doc5000": "lab-e2e-docente-fixpag5000",
             "doc300": "lab-e2e-docente-fixpag300"}
    tokens = {k: M.lab_token(env, u) for k, u in users.items()}
    for k, jwt in tokens.items():
        print(f"claims {k}:", json.dumps(M.summarize_claims(jwt)))
    g001, est_id = ctx["grupoDocenteSeed"], ctx["estudianteSeedId"]
    pdp = Pdp(env)
    pdp.ensure_role()
    pdp.grant(static_routes(ctx))

    # ---- 1. RBAC por rol (PDP concede todas las rutas; la capa local decide) --------------------
    for path, allowed, allowed_status in expected_rbac(ctx):
        for who in ("admin", "decano", "coord", "docente", "estudiante", "sinrol", "doc1000"):
            response = call(base, "GET", path, tokens[who])
            ok = who in allowed
            scope_denied = who == "doc1000" and path.startswith("/api/v1/grupos/")
            expected = allowed_status if ok else 403
            layer = M.L_ALLOW if ok else (M.L_SCOPE if scope_denied else M.L_ROLE)
            checks = [("X-Decision-Id: el PDP permitió la ruta", has_decision(response[1]))]
            if not ok:
                checks += M.json_error_checks(response[1], response[2], "FORBIDDEN", 403)
            row(f"RB-{who} GET {path.split('?')[0]}", expected, response, layer=layer, checks=checks)
    response = call(base, "GET", "/api/v1/admin/auditoria", tokens["admin"])
    row("RB-pdp-nogrant ADMINISTRADOR en ruta no concedida por el PDP", 403, response, layer=M.L_PEP,
        checks=[("sin X-Decision-Id (deniega el PEP)", not has_decision(response[1]))])

    # ---- 2. Volumen: 300 / 1000 / 5000 -------------------------------------------------------
    for size, who in ((300, "doc300"), (1000, "doc1000"), (5000, "doc5000")):
        started = time.monotonic()
        response = call(base, "GET", f"/api/v1/grupos/{FIX[size]}/estudiantes", tokens[who])
        elapsed = round(time.monotonic() - started, 2)
        data = data_of(response[2]) or []
        total = (M.parse_body(response[2]) or {}).get("total")
        row(f"VOL-{size} estudiantes del grupo propio FIX-PAG-{size}", 200, response, layer=M.L_ALLOW, count=len(data),
            extra=f"{elapsed}s", checks=[("count == matrículas SQL", len(data) == size), ("total == count", total == size)])
    for page, expected_items in ((0, 100), (49, 100), (50, 0)):
        response = call(base, "GET", f"/api/v1/estudiantes?grupoId={FIX[5000]}&page={page}&size=100", tokens["coord"])
        parsed = M.parse_body(response[2]) or {}
        row(f"VOL-DIR-p{page} directorio FIX-PAG-5000 página {page}", 200, response, layer=M.L_ALLOW,
            count=len(parsed.get("items") or []),
            checks=[("totalItems == 5000", parsed.get("totalItems") == 5000), ("totalPages == 50", parsed.get("totalPages") == 50),
                    (f"items == {expected_items}", len(parsed.get("items") or []) == expected_items)])
    response = call(base, "GET", f"/api/v1/estudiantes?grupoId={FIX[5000]}&page=0&size=100", tokens["doc5000"])
    row("VOL-DIR-docente directorio general (no se abre a DOCENTE)", 403, response, layer=M.L_ROLE)

    # ---- 3. Sesiones UTC v2 / v1 (escrituras en la DB aislada) ------------------------------
    run_id = uuid.uuid4().hex[:6]
    name = f"SEC003 E2E {run_id}"
    body = {"grupo": g001, "nombre": name, "fechaHoraInicio": "2026-11-03T08:00:00-05:00",
            "fechaHoraFin": "2026-11-03T10:00:00-05:00"}
    row("WR-01 POST /api/v2/sesiones (offset -05:00)", 201, call(base, "POST", "/api/v2/sesiones", tokens["docente"], body),
        layer=M.L_ALLOW)
    response = call(base, "GET", f"/api/v2/sesiones/grupo/{g001}", tokens["docente"])
    created = [s for s in (data_of(response[2]) or []) if s.get("nombre") == name]
    sid = created[0]["sesion"] if created else None
    row("WR-02 GET v2 por grupo: sesión creada en UTC", 200, response, layer=M.L_ALLOW, count=len(created),
        checks=[("una sesión con el nombre", len(created) == 1),
                ("inicio 13:00Z", bool(created) and created[0].get("fechaHoraInicio") == "2026-11-03T13:00:00Z"),
                ("fin 15:00Z", bool(created) and created[0].get("fechaHoraFin") == "2026-11-03T15:00:00Z"),
                ("procedencia UTC_V2", bool(created) and created[0].get("procedenciaTemporal") == "UTC_V2")])
    if not sid:
        raise SystemExit("la sesión de prueba no se creó; se detiene la matriz de escrituras")
    pdp.grant([("GET", f"/api/v2/sesiones/{sid}"), ("PATCH", f"/api/v2/sesiones/{sid}"),
               ("GET", f"/api/v1/sesiones/{sid}"), ("PUT", f"/api/v1/sesiones/{sid}"),
               ("PATCH", f"/api/v1/sesiones/{sid}")])
    response = call(base, "PATCH", f"/api/v2/sesiones/{sid}", tokens["docente"],
                    {"nombre": name, "fechaHoraInicio": "2026-11-03T15:00:00+02:00", "fechaHoraFin": "2026-11-03T18:30:00+02:00"})
    row("WR-03 PATCH v2 (offset +02:00)", 200, response, layer=M.L_ALLOW)
    response = call(base, "GET", f"/api/v2/sesiones/{sid}", tokens["docente"])
    d = data_of(response[2]) or {}
    row("WR-04 GET v2: instantes convertidos a UTC", 200, response, layer=M.L_ALLOW,
        checks=[("inicio 13:00Z", d.get("fechaHoraInicio") == "2026-11-03T13:00:00Z"),
                ("fin 16:30Z", d.get("fechaHoraFin") == "2026-11-03T16:30:00Z"),
                ("CONFIRMADA", d.get("estadoTemporal") == "CONFIRMADA")])
    response = call(base, "GET", f"/api/v1/sesiones/{sid}", tokens["docente"])
    d1 = data_of(response[2]) or {}
    # Contrato LocalSessionDateTime: wire sin offset, x-persistence-semantics UTC -> el instante UTC sin Z.
    row("WR-05 GET v1: mismo instante UTC sin offset (LocalSessionDateTime)", 200, response, layer=M.L_ALLOW,
        extra=f"v1 inicio={d1.get('fechaHoraInicio')} fin={d1.get('fechaHoraFin')}",
        checks=[("inicio 13:00 (UTC sin Z)", str(d1.get("fechaHoraInicio", "")).startswith("2026-11-03T13:00")),
                ("fin 16:30 (UTC sin Z)", str(d1.get("fechaHoraFin", "")).startswith("2026-11-03T16:30")),
                ("sin offset en el wire", not str(d1.get("fechaHoraInicio", "")).endswith("Z"))])
    response = call(base, "PUT", f"/api/v1/sesiones/{sid}", tokens["docente"],
                    {"nombre": name, "fechaHoraInicio": "2026-11-03T09:00:00", "fechaHoraFin": "2026-11-03T11:00:00"})
    row("WR-06 PUT v1 legacy (hora local)", 200, response, layer=M.L_ALLOW)
    response = call(base, "GET", f"/api/v2/sesiones/{sid}", tokens["docente"])
    d = data_of(response[2]) or {}
    row("WR-07 GET v2 tras escritura v1: INDETERMINADA (contrato UTC-D07)", 200, response, layer=M.L_ALLOW,
        checks=[("INDETERMINADA", d.get("estadoTemporal") == "INDETERMINADA"), ("inicio null", d.get("fechaHoraInicio") is None)])
    response = call(base, "PATCH", f"/api/v1/sesiones/{sid}", tokens["docente"],
                    {"nombre": name, "fechaHoraInicio": "2026-11-03T08:00:00", "fechaHoraFin": "2026-11-03T10:00:00"})
    row("WR-08 PATCH v1", 200, response, layer=M.L_ALLOW)
    response = call(base, "PATCH", f"/api/v2/sesiones/{sid}", tokens["docente"],
                    {"nombre": name, "fechaHoraInicio": "2026-11-03T13:00:00Z", "fechaHoraFin": "2026-11-03T15:00:00Z"})
    row("WR-09 PATCH v2 restablece CONFIRMADA", 200, response, layer=M.L_ALLOW)
    response = call(base, "PUT", f"/api/v2/sesiones/{sid}", tokens["docente"],
                    {"nombre": name, "fechaHoraInicio": "2026-11-03T13:00:00Z", "fechaHoraFin": "2026-11-03T15:00:00Z"})
    row("WR-10 PUT v2 inexistente (UTC-D09)", [403, 405], response, layer="PEP/PDP deny o Spring 405", extra="403 = el PDP no concede PUT v2 (deny-by-default); 405 = Spring")
    foreign = dict(body, grupo=FIX[300], nombre=f"SEC003 ajeno {run_id}")
    response = call(base, "POST", "/api/v2/sesiones", tokens["docente"], foreign)
    row("WR-11 POST v2 en grupo ajeno (FIX-PAG-300)", 403, response, layer=M.L_SCOPE,
        checks=M.json_error_checks(response[1], response[2], "FORBIDDEN", 403))
    row("WR-12 PATCH v2 de sesión ajena (docente FIX-PAG-1000)", 403,
        call(base, "PATCH", f"/api/v2/sesiones/{sid}", tokens["doc1000"],
             {"nombre": "x", "fechaHoraInicio": "2026-11-03T13:00:00Z", "fechaHoraFin": "2026-11-03T15:00:00Z"}),
        layer=M.L_SCOPE)
    row("WR-13 POST v2 con rol ESTUDIANTE", 403, call(base, "POST", "/api/v2/sesiones", tokens["estudiante"], body),
        layer=M.L_ROLE)
    row("WR-14 POST v2 sin token", 401, call(base, "POST", "/api/v2/sesiones", None, body), layer=M.L_ENTRY)
    after = readonly_query("SELECT COUNT(*) n FROM dbo.uv_sesion_v2 WHERE nombre LIKE @p", p=f"SEC003 ajeno {run_id}%")
    row("WR-15 sin escritura tras los 403/401", 200, (200, {}, b""), layer="SQL aislado (lectura)",
        checks=[("0 sesiones ajenas creadas", int(after[0]["n"]) == 0)])

    # ---- 4. Asistencias + reclamo (revisión) ------------------------------------------------
    lote = {"sesionId": sid, "registros": [{"estudianteId": est_id, "estado": "SJC"}]}
    row("AS-01 POST asistencias/lote por el docente titular", 201,
        call(base, "POST", "/api/v1/asistencias/lote", tokens["docente"], lote), layer=M.L_ALLOW)
    row("AS-02 POST asistencias/lote por docente ajeno", 403,
        call(base, "POST", "/api/v1/asistencias/lote", tokens["doc1000"], lote), layer=M.L_SCOPE)
    response = call(base, "GET", f"/api/v1/grupos/{g001}/asistencias?sesionId={sid}", tokens["docente"])
    regs = data_of(response[2]) or []
    row("AS-03 GET asistencias del grupo/sesión", 200, response, layer=M.L_ALLOW, count=len(regs),
        checks=[("registro del estudiante presente", len(regs) >= 1)])
    reclamo = {"sesionId": sid, "categoria": "SALUD", "justificacion": f"SEC-BASELINE-003 E2E {run_id}"}
    row("RC-01 POST revisión por docente (rol no permitido)", 403,
        call(base, "POST", "/api/v1/asistencias/revisiones", tokens["docente"], reclamo), layer=M.L_ROLE)
    row("RC-02 POST revisión por el estudiante", 202,
        call(base, "POST", "/api/v1/asistencias/revisiones", tokens["estudiante"], reclamo), layer=M.L_ALLOW)
    found = readonly_query("SELECT CAST(r.id AS char(36)) id FROM dbo.uv_solicitud_revision_asistencia r "
                           "JOIN dbo.uv_asistencia a ON a.id = r.idAsistencia WHERE a.idSesion = @s "
                           "AND r.justificacionSolicitud = @j", s=sid, j=reclamo["justificacion"])
    rid = found[0]["id"].strip().lower() if found else None
    pdp.grant([("GET", "/api/v1/docente/reclamos")])
    row("RC-03 GET /api/v1/docente/reclamos (listado del docente)", 501,
        call(base, "GET", "/api/v1/docente/reclamos", tokens["docente"]), layer="Controller (FEATURE_UNAVAILABLE)",
        extra="501 = FEATURE_UNAVAILABLE: el docente no puede listar reclamos por API; id localizado en la DB aislada"
        if rid else "reclamo no localizado")
    if rid:
        pdp.grant([("PATCH", f"/api/v1/docente/reclamos/{rid}"), ("DELETE", f"/api/v1/estudiante/reclamos/{rid}")])
        resolve = {"accion": "APROBADA", "respuestaDocente": "Aprobada en E2E SEC-BASELINE-003"}
        row("RC-04 PATCH reclamo por docente ajeno", 403,
            call(base, "PATCH", f"/api/v1/docente/reclamos/{rid}", tokens["doc1000"], resolve), layer=M.L_SCOPE)
        row("RC-05 PATCH reclamo por el docente titular", 200,
            call(base, "PATCH", f"/api/v1/docente/reclamos/{rid}", tokens["docente"], resolve), layer=M.L_ALLOW)
        state = readonly_query("SELECT nombreEstado FROM dbo.uv_solicitud_revision_asistencia WHERE id = @r", r=rid)
        row("RC-06 estado persistido tras aprobar", 200, (200, {}, b""), layer="SQL aislado (lectura)",
            extra=f"estado={state[0]['nombreEstado'] if state else None}", checks=[("estado leído", bool(state))])
        # ---- 5. DELETE: no hay DELETE de negocio implementado (501) --------------------------
        response = call(base, "DELETE", f"/api/v1/estudiante/reclamos/{rid}", tokens["estudiante"])
        row("DEL-01 DELETE reclamo por el estudiante", 501, response, layer="Controller (FEATURE_UNAVAILABLE)",
            checks=[("X-Decision-Id: el PDP permitió", has_decision(response[1]))])
        row("DEL-02 DELETE reclamo por docente (rol)", 403,
            call(base, "DELETE", f"/api/v1/estudiante/reclamos/{rid}", tokens["docente"]), layer=M.L_ROLE)
    row("DEL-03 DELETE matrícula por COORDINADOR", 501,
        call(base, "DELETE", f"/api/v1/grupos/{g001}/estudiantes/{est_id}", tokens["coord"]), layer="Controller (FEATURE_UNAVAILABLE)")
    row("DEL-04 DELETE matrícula por DOCENTE (rol)", 403,
        call(base, "DELETE", f"/api/v1/grupos/{g001}/estudiantes/{est_id}", tokens["docente"]), layer=M.L_ROLE)
    left = readonly_query("SELECT COUNT(*) n FROM dbo.uv_estudiante_grupo WHERE idGrupo = @g AND idEstudiante = @e",
                          g=g001, e=est_id)
    row("DEL-05 matrícula intacta tras los DELETE", 200, (200, {}, b""), layer="SQL aislado (lectura)",
        checks=[("matrícula presente", int(left[0]["n"]) == 1)])

    # ---- 6. Descargas ------------------------------------------------------------------------
    xlsx = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    for case, who, expected, layer in (("DL-01 reporte Excel por el docente titular", "docente", 200, M.L_ALLOW),
                                       ("DL-02 reporte Excel por COORDINADOR", "coord", 200, M.L_ALLOW),
                                       ("DL-03 reporte Excel por docente AJENO (titularidad)", "doc1000", 403, M.L_SCOPE),
                                       ("DL-04 reporte Excel por ESTUDIANTE (rol)", "estudiante", 403, M.L_ROLE)):
        response = call(base, "GET", f"/api/v1/grupos/{g001}/reportes/asistencia-excel", tokens[who],
                        headers={"Accept": f"{xlsx}, application/json"})
        checks = [("xlsx", xlsx in lower(response[1]).get("content-type", ""))] if expected == 200 else []
        row(case, expected, response, layer=layer, checks=checks,
            extra=f"bytes={len(response[2] or b'')}")
    pdf = b"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[]/Count 0>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n"
    response = multipart(base, "/api/v1/archivos/subir", tokens["estudiante"], "archivo", f"soporte-{run_id}.pdf",
                         "application/pdf", pdf)
    file_id = ((M.parse_body(response[2]) or {}).get("datos") or {}).get("fileId")
    row("DL-05 subir soporte (ESTUDIANTE, ClamAV + MinIO)", 201, response, layer=M.L_ALLOW, extra=f"fileId={'sí' if file_id else 'no'}")
    row("DL-06 subir soporte con rol DOCENTE", 403,
        multipart(base, "/api/v1/archivos/subir", tokens["docente"], "archivo", "x.pdf", "application/pdf", pdf), layer=M.L_ROLE)
    if file_id:
        pdp.grant([("GET", f"/api/v1/archivos/{file_id}")])
        response = call(base, "GET", f"/api/v1/archivos/{file_id}", tokens["estudiante"], headers={"Accept": "*/*"})
        row("DL-07 descargar soporte propio", 200, response, layer=M.L_ALLOW,
            checks=[("mismo contenido", response[2] == pdf)])
        row("DL-08 descargar soporte ajeno (DOCENTE titular)", 404,
            call(base, "GET", f"/api/v1/archivos/{file_id}", tokens["docente"], headers={"Accept": "*/*"}),
            layer=M.L_SCOPE, extra="contrato LB-004: 404 para no propietario, cualquier rol")

    # ---- 7. CORS / CSRF ----------------------------------------------------------------------
    pre = {"Access-Control-Request-Method": "POST", "Access-Control-Request-Headers": "authorization,content-type"}
    response = L.http(L.plain_opener(), "OPTIONS", f"{base}/api/v2/sesiones", headers={"Origin": ORIGIN_OK, **pre})
    row("CO-01 preflight desde origen permitido", 200, response, layer="Spring CorsFilter",
        checks=[("ACAO == origen", lower(response[1]).get("access-control-allow-origin") == ORIGIN_OK),
                ("PEP no consultado", not has_decision(response[1]))])
    response = L.http(L.plain_opener(), "OPTIONS", f"{base}/api/v2/sesiones", headers={"Origin": ORIGIN_BAD, **pre})
    row("CO-02 preflight desde origen no permitido", 403, response, layer="Spring CorsFilter",
        checks=[("sin ACAO", "access-control-allow-origin" not in lower(response[1]))])
    response = call(base, "POST", "/api/v2/sesiones", None, dict(body, nombre=f"SEC003 csrf {run_id}"),
                    headers={"Cookie": "JSESSIONID=forged; XSRF-TOKEN=forged"})
    row("CS-01 POST con cookies y sin Bearer (CSRF, sin Origin)", 401, response, layer=M.L_ENTRY,
        checks=[("sin Set-Cookie", "set-cookie" not in lower(response[1])),
                ("401 de autenticación, no 403 de CSRF", M.parse_body(response[2]).get("code") == "UNAUTHORIZED")])
    response = call(base, "POST", "/api/v2/sesiones", None, dict(body, nombre=f"SEC003 csrf {run_id}"),
                    headers={"Origin": ORIGIN_BAD, "Cookie": "JSESSIONID=forged; XSRF-TOKEN=forged"})
    row("CS-01b POST con cookies desde origen no permitido", 403, response, layer="Spring CorsFilter",
        checks=[("sin ACAO", "access-control-allow-origin" not in lower(response[1]))])
    response = call(base, "POST", "/api/v2/sesiones", tokens["docente"], dict(body, nombre=f"SEC003 cors {run_id}"),
                    headers={"Origin": ORIGIN_BAD})
    row("CS-02 POST con Bearer desde origen no permitido", 403, response, layer="Spring CorsFilter")
    response = call(base, "GET", "/api/v1/docente/horarios", tokens["docente"], headers={"Origin": ORIGIN_OK})
    row("CS-03 respuesta autenticada sin sesión ni cookies", 200, response, layer=M.L_ALLOW,
        checks=[("sin Set-Cookie", "set-cookie" not in lower(response[1])),
                ("ACAO == origen", lower(response[1]).get("access-control-allow-origin") == ORIGIN_OK)])
    leaked = readonly_query("SELECT COUNT(*) n FROM dbo.uv_sesion_v2 WHERE nombre IN (@a, @b)",
                            a=f"SEC003 csrf {run_id}", b=f"SEC003 cors {run_id}")
    row("CS-04 ninguna sesión creada por CS-01/CS-02", 200, (200, {}, b""), layer="SQL aislado (lectura)",
        checks=[("0 filas", int(leaked[0]["n"]) == 0)])

    # ---- 8. SSE por rol (titularidad solo de docentes) ---------------------------------------
    for who, expected in (("docente", 200), ("estudiante", 403), ("coord", 403)):
        response = M.sse_open(base, f"/api/v1/realtime/stream?grupoId={g001}", tokens[who])
        checks = [("text/event-stream", "text/event-stream" in lower(response[1]).get("content-type", ""))] \
            if expected == 200 else M.json_error_checks(response[1], response[2], "FORBIDDEN", 403)
        row(f"SSE-{who} stream del grupo semilla", expected, response,
            layer=M.L_ALLOW if expected == 200 else M.L_SCOPE, checks=checks)
    print("guard:", guard)


# --------------------------------------------------------------------------- revocation

def revocation(base):
    import provision_shared_identity_e2e as P
    env = L.read_env()
    admin = P.Admin(env)
    username = "lab-e2e-revocable"
    user = admin.call("GET", f"/users?username={username}&exact=true")[0]
    token = M.lab_token(env, username)
    row("REV-01 token vigente antes de revocar", 200, call(base, "GET", "/api/v1/docente/horarios", token), layer=M.L_ALLOW)
    admin.call("POST", f"/users/{user['id']}/logout")
    response = call(base, "GET", "/api/v1/docente/horarios", token)
    row("REV-02 mismo token tras cerrar todas las sesiones del usuario", [401, 403], response,
        layer="JwtDecoder local + PEP/PDP", extra=f"X-Decision-Id={'sí' if has_decision(response[1]) else 'no'}")
    user["enabled"] = False
    admin.call("PUT", f"/users/{user['id']}", user)
    try:
        status, _, raw = L.http(L.plain_opener(), "POST", f"{ISSUER}/protocol/openid-connect/token",
                                {"grant_type": "password", "client_id": E2E_CLIENT, "client_secret": env["LAB_E2E_CLIENT_SECRET"],
                                 "username": username, "password": env["LAB_USER_LAB_E2E_REVOCABLE_PASSWORD"],
                                 "scope": "openid"}, form=True)
        row("REV-03 nuevo login de usuario deshabilitado", [400, 401], (status, {}, raw), layer="Keycloak",
            extra=f"error={(M.parse_body(raw) or {}).get('error')}")
        response = call(base, "GET", "/api/v1/docente/horarios", token)
        row("REV-04 token previo de usuario deshabilitado", [401, 403], response, layer="JwtDecoder local + PEP/PDP",
            extra=f"X-Decision-Id={'sí' if has_decision(response[1]) else 'no'}")
    finally:
        user["enabled"] = True
        admin.call("PUT", f"/users/{user['id']}", user)
    fresh = M.lab_token(env, username)
    row("REV-05 usuario rehabilitado (estado del laboratorio restablecido)", 200,
        call(base, "GET", "/api/v1/docente/horarios", fresh), layer=M.L_ALLOW)


def readonly_probe(base):
    """Backend arrancado con -DatasourceEnv e2e-sql-readonly.env: una escritura válida para la API debe
    fallar en SQL Server (permiso denegado) sin crear filas. Demuestra el privilegio mínimo extremo a extremo."""
    port = int(base.rsplit(":", 1)[1].split("/")[0])
    isolated, guard = assert_isolated(port)
    if not isolated or "asistencias_e2e_readonly@" not in guard:
        raise SystemExit("BLOCKED: el backend no usa el login de solo lectura de la DB aislada")
    ctx = load_verification()["context"]
    env = L.read_env()
    docente = M.lab_token(env, "lab-e2e-docente-seed")
    row("RO-HTTP-01 lectura con login de solo lectura", 200,
        call(base, "GET", f"/api/v2/sesiones/grupo/{ctx['grupoDocenteSeed']}", docente), layer=M.L_ALLOW)
    name = f"SEC003 readonly {uuid.uuid4().hex[:6]}"
    response = call(base, "POST", "/api/v2/sesiones", docente,
                    {"grupo": ctx["grupoDocenteSeed"], "nombre": name, "fechaHoraInicio": "2026-11-04T08:00:00-05:00",
                     "fechaHoraFin": "2026-11-04T10:00:00-05:00"})
    created = readonly_query("SELECT COUNT(*) n FROM dbo.uv_sesion_v2 WHERE nombre = @n", n=name)
    row("RO-HTTP-02 escritura con login de solo lectura: SQL Server la deniega", [500, 503], response,
        layer="SQL Server (permiso EXECUTE denegado)",
        checks=[("0 filas creadas", int(created[0]["n"]) == 0), ("sin éxito 2xx", response[0] >= 400)])
    print("guard:", guard)


# --------------------------------------------------------------------------- main

def exit_code(rows):
    if any(r["verdict"] == M.FAIL for r in rows):
        return 1
    if any(r["verdict"] != M.PASS for r in rows):
        return 2
    return 0


def selftest():
    import unittest

    class Harness(unittest.TestCase):
        def test_design_limit_never_pass(self):
            M.results.clear()
            r = row("REV-02 x", [401, 403], (200, {}, b""))
            self.assertEqual(r["verdict"], "KNOWN_DESIGN_LIMIT")
            self.assertEqual(exit_code(M.results), 2)

        def test_known_defect_never_pass(self):
            M.results.clear()
            self.assertEqual(row("DL-03 x", 403, (200, {}, b""))["verdict"], "KNOWN_DEFECT")

        def test_fail_dominates(self):
            M.results.clear()
            row("REV-02 x", 401, (200, {}, b""))
            row("ZZ-01 x", 200, (500, {}, b""))
            self.assertEqual(exit_code(M.results), 1)

        def test_pass_when_expected(self):
            M.results.clear()
            self.assertEqual(row("DL-03 x", 403, (403, {}, b""))["verdict"], M.PASS)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(Harness)
    ok = unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful()
    M.results.clear()
    return 0 if ok else 1


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "run"
    target = sys.argv[2] if len(sys.argv) > 2 else "http://127.0.0.1:8083"
    if mode == "selftest":
        sys.exit(selftest())
    if mode == "provision":
        provision()
        sys.exit(0)
    {"run": run, "revocation": revocation, "readonly-probe": readonly_probe}[mode](target)
    counts = {}
    for r in M.results:
        counts[r["verdict"]] = counts.get(r["verdict"], 0) + 1
    print("TOTAL=%d %s" % (len(M.results), " ".join(f"{k}={v}" for k, v in sorted(counts.items()))))
    sys.exit(exit_code(M.results))
