"""SEC-BASELINE-002 — identidad compartida de LABORATORIO (Keycloak 19090 + PDP), idempotente.

Uso: python -I provision_shared_identity_e2e.py

Precondición: verify-academic-identity.ps1 -Mode Verify (solo lectura en SQL Server) generó
%USERPROFILE%\\.asistencias-security-lab\\academic-identity-verification.json hace menos de 24 h y
marcó como verificado cada idUsuario que se asocia aquí. Sin esa evidencia no se asocia ningún UUID.

Keycloak (realm security-baseline, solo laboratorio):
  * User Profile: atributo idUsuario (UUID, view user/admin, edit solo admin), igual que el realm
    institucional.
  * Cliente de recurso asistencias-api (sin flujos) con client roles DOCENTE y ESTUDIANTE.
  * Cliente asistencias-lab-e2e: emite aud security-baseline-pdp (scope existente
    security-baseline-claims, sin modificarlo) + asistencias-api, resource_access.asistencias-api.roles
    e idUsuario (mappers oficiales). Direct access grants solo para automatizar el laboratorio.
  * Cliente asistencias-lab-e2e-wrong-aud: igual pero SIN audiencia asistencias-api (caso negativo).
  * Usuarios nuevos lab-e2e-* (contraseñas aleatorias en lab.env); no toca lab-docente-*.
PDP (aplicación existente asistencias-uco-lab): recursos GET exactos, rol nuevo
DOCENTE_E2E_FIXPAG300_LAB y asignaciones. No modifica recursos, roles ni asignaciones existentes.

Nunca imprime contraseñas, secretos, credenciales ni tokens. NO es una política de identidad
productiva: la asociación idUsuario -> usuario del laboratorio es manual y experimental.
"""
import base64
import datetime as dt
import json
import os
import re
import sys
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402

VERIFICATION = os.path.join(os.path.dirname(L.ENV_PATH), "academic-identity-verification.json")
EVIDENCE_DIR = os.path.dirname(L.ENV_PATH)
UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
ISSUER = f"{L.KEYCLOAK}/realms/{L.REALM}"
API_CLIENT = "asistencias-api"
API_ROLES = ["DOCENTE", "ESTUDIANTE"]
E2E_CLIENT = "asistencias-lab-e2e"
WRONG_AUD_CLIENT = "asistencias-lab-e2e-wrong-aud"
PDP_SCOPE = "security-baseline-claims"
APPLICATION = "asistencias-uco-lab"
PDP_ROLE = "DOCENTE_E2E_FIXPAG300_LAB"

GROUP_OWN = "de000005-0000-4000-8000-000000000001"      # FIX-PAG-300
GROUP_FOREIGN = "de000005-0000-4000-8000-000000000002"  # FIX-PAG-1000

# username, idUsuario (None = sin claim), client role asistencias-api, asignación al rol PDP nuevo
USERS = [
    ("lab-e2e-docente-fixpag300", "de000003-0000-4000-8000-000000000001", "DOCENTE", True),
    ("lab-e2e-rol-incorrecto", "de000003-0000-4000-8000-000000000002", "ESTUDIANTE", True),
    ("lab-e2e-docente-sin-pdp", "de000003-0000-4000-8000-000000000003", "DOCENTE", False),
    ("lab-e2e-sin-idusuario", None, "DOCENTE", True),
]

# Rutas EXACTAS (el PDP v0.1.6 devuelve 500 con plantillas {x}). No son la solución definitiva para
# grupos dinámicos; ver SEC-BASELINE-002 SECURITY_CONTRACT_MATRIX.md.
RESOURCES = [
    ("/api/v1/docente/horarios", "GET"),                         # existente (SEC-BASELINE-001)
    ("/api/v1/realtime/stream", "GET"),                          # existente (SEC-BASELINE-001)
    (f"/api/v1/grupos/{GROUP_OWN}/estudiantes", "GET"),
    (f"/api/v2/sesiones/grupo/{GROUP_OWN}", "GET"),
    (f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes", "GET"),      # ajeno, concedido a propósito
    (f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}", "GET"),          # ajeno, concedido a propósito
]


def load_verification():
    if not os.path.exists(VERIFICATION):
        raise SystemExit("BLOCKED: falta la verificación SQL (verify-academic-identity.ps1 -Mode Verify)")
    with open(VERIFICATION, encoding="utf-8-sig") as handle:
        evidence = json.load(handle)
    verified_at = dt.datetime.fromisoformat(evidence["verifiedAt"].replace("Z", "+00:00")[:26] + "+00:00")
    age = dt.datetime.now(dt.timezone.utc) - verified_at
    if age > dt.timedelta(hours=24):
        raise SystemExit(f"BLOCKED: verificación SQL con {age} de antigüedad; repetirla")
    return {i["idUsuario"].lower(): i for i in evidence["identities"] if i.get("verified") is True}, evidence


class Admin:
    def __init__(self, env):
        self.headers = {"Authorization": f"Bearer {L.kc_admin_token(env)}"}
        self.base = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def call(self, method, path, body=None, ok=(200, 201, 204)):
        status, headers, raw = L.http(L.plain_opener(), method, self.base + path, body, headers=self.headers)
        if status not in ok:
            raise SystemExit(f"Keycloak admin {method} {path.split('?')[0]}: HTTP {status}")
        return json.loads(raw) if raw else None

    def client(self, client_id):
        found = self.call("GET", f"/clients?clientId={client_id}")
        return found[0] if found else None

    def scope_id(self, name):
        return next(s["id"] for s in self.call("GET", "/client-scopes") if s["name"] == name)


def ensure_user_profile(admin):
    profile = admin.call("GET", "/users/profile")
    desired = {"name": "idUsuario", "displayName": "ID Usuario",
               "validations": {"pattern": {"pattern": UUID_PATTERN,
                                           "error-message": "El idUsuario debe tener formato UUID."}},
               "annotations": {}, "permissions": {"view": ["user", "admin"], "edit": ["admin"]},
               "multivalued": False}
    current = next((a for a in profile["attributes"] if a["name"] == "idUsuario"), None)
    if current and current.get("permissions") == desired["permissions"] \
            and current.get("validations") == desired["validations"] and not current.get("required"):
        print("user-profile idUsuario: OK")
        return
    profile["attributes"] = [a for a in profile["attributes"] if a["name"] != "idUsuario"] + [desired]
    admin.call("PUT", "/users/profile", profile)
    print("user-profile idUsuario: UPDATED (UUID, edit=admin)")


def mapper(name, kind, config):
    return {"name": name, "protocol": "openid-connect", "protocolMapper": kind, "config": config}


def e2e_mappers(include_api_audience):
    mappers = [
        mapper("idUsuario", "oidc-usermodel-attribute-mapper", {
            "user.attribute": "idUsuario", "claim.name": "idUsuario", "jsonType.label": "String",
            "id.token.claim": "false", "access.token.claim": "true", "userinfo.token.claim": "false",
            "introspection.token.claim": "true", "multivalued": "false"}),
        mapper("asistencias-api-client-roles", "oidc-usermodel-client-role-mapper", {
            "usermodel.clientRoleMapping.clientId": API_CLIENT,
            "claim.name": "resource_access.${client_id}.roles", "jsonType.label": "String",
            "multivalued": "true", "id.token.claim": "false", "access.token.claim": "true",
            "userinfo.token.claim": "false", "introspection.token.claim": "true"}),
    ]
    if include_api_audience:
        mappers.append(mapper("audience-asistencias-api", "oidc-audience-mapper", {
            "included.client.audience": API_CLIENT, "id.token.claim": "false", "access.token.claim": "true",
            "introspection.token.claim": "true", "userinfo.token.claim": "false"}))
    return mappers


def ensure_api_client(admin):
    client = admin.client(API_CLIENT)
    if client is None:
        admin.call("POST", "/clients", {
            "clientId": API_CLIENT, "name": "AsistenciasUCO API (laboratorio)", "enabled": True,
            "description": "SEC-BASELINE-002: namespace de client roles; sin login. Solo laboratorio.",
            "protocol": "openid-connect", "publicClient": False, "standardFlowEnabled": False,
            "directAccessGrantsEnabled": False, "implicitFlowEnabled": False,
            "serviceAccountsEnabled": False, "redirectUris": [], "webOrigins": []})
        client = admin.client(API_CLIENT)
        print(f"client {API_CLIENT}: CREATED")
    else:
        print(f"client {API_CLIENT}: exists")
    existing = {r["name"] for r in admin.call("GET", f"/clients/{client['id']}/roles")}
    for role in API_ROLES:
        if role not in existing:
            admin.call("POST", f"/clients/{client['id']}/roles", {"name": role, "description": f"Laboratorio: {role}"})
    roles = {r["name"]: r for r in admin.call("GET", f"/clients/{client['id']}/roles")}
    print(f"client {API_CLIENT} roles: {sorted(roles)}")
    return client, roles


def ensure_token_client(admin, env, client_id, secret_key, include_api_audience, api_client, api_roles):
    secret = env.get(secret_key)
    if not secret:
        secret = L.random_secret(48)
        L.write_env_value(secret_key, secret)
        env[secret_key] = secret
    client = admin.client(client_id)
    if client is None:
        admin.call("POST", "/clients", {
            "clientId": client_id, "enabled": True, "protocol": "openid-connect",
            "description": "SEC-BASELINE-002: emisor de tokens de LABORATORIO para el E2E. No productivo.",
            "publicClient": False, "clientAuthenticatorType": "client-secret", "secret": secret,
            "standardFlowEnabled": False, "implicitFlowEnabled": False, "serviceAccountsEnabled": False,
            "directAccessGrantsEnabled": True, "fullScopeAllowed": False, "redirectUris": [], "webOrigins": [],
            "defaultClientScopes": [], "optionalClientScopes": [],
            "protocolMappers": e2e_mappers(include_api_audience)})
        client = admin.client(client_id)
        print(f"client {client_id}: CREATED")
    else:
        print(f"client {client_id}: exists")
    rep = admin.call("GET", f"/clients/{client['id']}")
    rep.update({"secret": secret, "fullScopeAllowed": False, "directAccessGrantsEnabled": True,
                "standardFlowEnabled": False, "serviceAccountsEnabled": False, "publicClient": False})
    admin.call("PUT", f"/clients/{client['id']}", rep)
    stored = admin.call("GET", f"/clients/{client['id']}/client-secret")
    if stored.get("value") != secret:
        raise SystemExit(f"client {client_id}: secret mismatch")
    have = {m["name"]: m for m in admin.call("GET", f"/clients/{client['id']}/protocol-mappers/models")}
    wanted = {m["name"]: m for m in e2e_mappers(include_api_audience)}
    for name, model in wanted.items():
        if name not in have:
            admin.call("POST", f"/clients/{client['id']}/protocol-mappers/models", model)
        elif have[name]["config"] != model["config"]:
            model["id"] = have[name]["id"]
            admin.call("PUT", f"/clients/{client['id']}/protocol-mappers/models/{have[name]['id']}", model)
    for name in set(have) - set(wanted):
        admin.call("DELETE", f"/clients/{client['id']}/protocol-mappers/models/{have[name]['id']}")
    # Única client scope por defecto: la existente del PDP (aud security-baseline-pdp, sub, tenant), sin modificarla.
    scope_id = admin.scope_id(PDP_SCOPE)
    defaults = {s["name"]: s["id"] for s in admin.call("GET", f"/clients/{client['id']}/default-client-scopes")}
    if PDP_SCOPE not in defaults:
        admin.call("PUT", f"/clients/{client['id']}/default-client-scopes/{scope_id}")
    for name, sid in defaults.items():
        if name != PDP_SCOPE:
            admin.call("DELETE", f"/clients/{client['id']}/default-client-scopes/{sid}")
    # fullScopeAllowed=false: el token solo puede llevar estos client roles de asistencias-api.
    mapped = {r["name"] for r in admin.call("GET", f"/clients/{client['id']}/scope-mappings/clients/{api_client['id']}")}
    missing = [api_roles[r] for r in API_ROLES if r not in mapped]
    if missing:
        admin.call("POST", f"/clients/{client['id']}/scope-mappings/clients/{api_client['id']}", missing)
    print(f"client {client_id}: secret verified, mappers={sorted(wanted)}, default scope={PDP_SCOPE}, "
          f"role scope={API_ROLES}, fullScopeAllowed=false")
    return client


def ensure_user(admin, env, username, id_usuario, role, verified, api_client, api_roles):
    user_id, password_key = L.ensure_lab_user(env, admin.headers["Authorization"].split(" ", 1)[1],
                                              username, "LabE2E", username.replace("lab-e2e-", ""))
    rep = admin.call("GET", f"/users/{user_id}")
    attributes = dict(rep.get("attributes") or {})
    if id_usuario is not None:
        if id_usuario not in verified:
            raise SystemExit(f"BLOCKED: {id_usuario} no está verificado en SQL; no se asocia a {username}")
        attributes["idUsuario"] = [id_usuario]
    else:
        attributes.pop("idUsuario", None)
    rep["attributes"] = attributes
    rep["emailVerified"] = True
    rep["requiredActions"] = []
    admin.call("PUT", f"/users/{user_id}", rep)
    stored = (admin.call("GET", f"/users/{user_id}").get("attributes") or {}).get("idUsuario")
    if (stored or [None])[0] != id_usuario:
        raise SystemExit(f"{username}: idUsuario no quedó como se esperaba")
    current = {r["name"] for r in admin.call("GET", f"/users/{user_id}/role-mappings/clients/{api_client['id']}")}
    extra = [api_roles[r] for r in current if r != role]
    if extra:
        admin.call("DELETE", f"/users/{user_id}/role-mappings/clients/{api_client['id']}", extra)
    if role not in current:
        admin.call("POST", f"/users/{user_id}/role-mappings/clients/{api_client['id']}", [api_roles[role]])
    print(f"user {username}: kc_id={user_id} idUsuario={'<ninguno>' if id_usuario is None else id_usuario} "
          f"asistencias-api role={role}")
    return user_id, password_key


def provision_pdp(env, password_keys):
    for username, key in password_keys.items():
        _, _, status, session, _ = L.bff_session(env, username, key)
        print(f"pdp login {username}: BFF session HTTP {status}")
    opener, jar, _, _, _ = L.bff_session(env, "lab-pdp-admin", "LAB_USER_LAB_PDP_ADMIN_PASSWORD")
    headers = L.xsrf_headers(jar)

    def call(method, path, body=None, ok=(200, 201)):
        status, _, raw = L.http(opener, method, f"{L.PDP}{path}", body, headers=headers)
        parsed = json.loads(raw) if raw else {}
        if status not in ok:
            raise SystemExit(f"PDP {method} {path}: HTTP {status} {parsed.get('code')}")
        return parsed

    apps = call("GET", "/api/v1/applications?size=100")["data"]["content"]
    app_id = next(a["id"] for a in apps if a["name"] == APPLICATION)
    resources = call("GET", f"/api/v1/applications/{app_id}/resources").get("data") or []
    by_key = {(r["path"], r["method"]): r["id"] for r in resources}
    for path, method in RESOURCES:
        if (path, method) not in by_key:
            by_key[(path, method)] = call("POST", f"/api/v1/applications/{app_id}/resources",
                                          {"path": path, "method": method})["data"]["id"]
            print(f"pdp resource {method} {path}: CREATED {by_key[(path, method)]}")
        else:
            print(f"pdp resource {method} {path}: exists {by_key[(path, method)]}")
    roles = call("GET", f"/api/v1/applications/{app_id}/security/roles?size=100")["data"]["content"]
    role = next((r for r in roles if r["name"] == PDP_ROLE), None)
    role_id = role["id"] if role else call("POST", "/api/v1/roles", {
        "name": PDP_ROLE, "scope": "APPLICATION", "applicationId": app_id})["data"]["id"]
    print(f"pdp role {PDP_ROLE}: {role_id}")
    for key in RESOURCES:
        call("POST", f"/api/v1/roles/{role_id}/resources", {"roleId": role_id, "resourceId": by_key[key]})
    print(f"pdp grants {PDP_ROLE}: {len(RESOURCES)} recursos GET exactos")
    users = call("GET", "/api/v1/users?size=200")["data"]
    assigned = (call("GET", f"/api/v1/roles/{role_id}/assignments?size=100").get("data") or {}).get("content", [])
    assigned_ids = {a.get("userId") for a in assigned}
    pdp_ids = {}
    for username, _, _, assign in USERS:
        pdp_user = next(u for u in users if u["email"].startswith(f"{username}@"))
        pdp_ids[username] = pdp_user["id"]
        if assign and pdp_user["id"] not in assigned_ids:
            call("POST", f"/api/v1/roles/{role_id}/assignments",
                 {"roleId": role_id, "userId": pdp_user["id"], "applicationId": app_id})
        if not assign and pdp_user["id"] in assigned_ids:
            raise SystemExit(f"{username} no debe tener asignación PDP")
        print(f"pdp user {username}: pdp_id={pdp_user['id']} assigned={assign}")
    summary = call("GET", f"/api/v1/applications/{app_id}/security/summary")["data"]["counts"]
    print("pdp summary:", json.dumps(summary))
    return {"applicationId": app_id, "roleId": role_id, "resources": {f"{m} {p}": by_key[(p, m)] for p, m in RESOURCES},
            "pdpUserIds": pdp_ids, "summary": summary}


def token(env, client_id, secret_key, username, password_key):
    status, _, raw = L.http(L.plain_opener(), "POST", f"{ISSUER}/protocol/openid-connect/token",
                            {"grant_type": "password", "client_id": client_id, "client_secret": env[secret_key],
                             "username": username, "password": env[password_key], "scope": "openid"}, form=True)
    if status != 200:
        raise SystemExit(f"token {client_id}/{username}: HTTP {status}")
    return json.loads(raw)["access_token"]


def verify_claims(env, kc_ids, password_keys):
    _, _, raw = L.http(L.plain_opener(), "GET", f"{ISSUER}/protocol/openid-connect/certs")
    kids = {k["kid"] for k in json.loads(raw)["keys"] if k.get("use") == "sig"}
    rows = []
    for client_id, secret_key in ((E2E_CLIENT, "LAB_E2E_CLIENT_SECRET"), (WRONG_AUD_CLIENT, "LAB_E2E_WRONG_AUD_CLIENT_SECRET")):
        for username, id_usuario, role, _ in USERS:
            if client_id == WRONG_AUD_CLIENT and username != "lab-e2e-docente-fixpag300":
                continue
            jwt = token(env, client_id, secret_key, username, password_keys[username])
            header = json.loads(base64.urlsafe_b64decode(jwt.split(".")[0] + "=" * (-len(jwt.split(".")[0]) % 4)))
            claims = L.jwt_claims(jwt)
            aud = claims.get("aud") if isinstance(claims.get("aud"), list) else [claims.get("aud")]
            roles = ((claims.get("resource_access") or {}).get(API_CLIENT) or {}).get("roles")
            row = {"client": client_id, "user": username, "alg": header.get("alg"), "kidInJwks": header.get("kid") in kids,
                   "iss": claims.get("iss"), "aud": sorted(a for a in aud if a), "azp": claims.get("azp"),
                   "roles": roles, "idUsuario": claims.get("idUsuario"), "sub": claims.get("sub"),
                   "subIsKeycloakId": claims.get("sub") == kc_ids[username],
                   "subDiffersFromIdUsuario": claims.get("sub") != claims.get("idUsuario"),
                   "lifetimeSeconds": claims.get("exp", 0) - claims.get("iat", 0), "tenant": claims.get("tenant")}
            checks = [row["alg"] == "RS256", row["kidInJwks"], row["iss"] == ISSUER, "security-baseline-pdp" in row["aud"],
                      row["roles"] == [role], row["idUsuario"] == id_usuario, row["subIsKeycloakId"],
                      row["subDiffersFromIdUsuario"], 0 < row["lifetimeSeconds"] <= 300]
            checks.append(("asistencias-api" in row["aud"]) == (client_id == E2E_CLIENT))
            row["verdict"] = "PASS" if all(checks) else "FAIL"
            rows.append(row)
            print("claims:", json.dumps(row))
    return rows


def main():
    verified, sql_evidence = load_verification()
    env = L.read_env()
    admin = Admin(env)
    ensure_user_profile(admin)
    api_client, api_roles = ensure_api_client(admin)
    ensure_token_client(admin, env, E2E_CLIENT, "LAB_E2E_CLIENT_SECRET", True, api_client, api_roles)
    ensure_token_client(admin, env, WRONG_AUD_CLIENT, "LAB_E2E_WRONG_AUD_CLIENT_SECRET", False, api_client, api_roles)
    kc_ids, password_keys = {}, {}
    for username, id_usuario, role, _ in USERS:
        kc_ids[username], password_keys[username] = ensure_user(admin, env, username, id_usuario, role, verified,
                                                                api_client, api_roles)
    pdp = provision_pdp(env, password_keys)
    claims = verify_claims(env, kc_ids, password_keys)
    evidence = {"workItem": "SEC-BASELINE-002", "provisionedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
                "sqlVerification": {"verifiedAt": sql_evidence["verifiedAt"], "source": sql_evidence["source"],
                                    "verified": sorted(verified)},
                "keycloak": {"issuer": ISSUER, "clients": [API_CLIENT, E2E_CLIENT, WRONG_AUD_CLIENT],
                             "users": {u: {"keycloakId": kc_ids[u], "idUsuario": i, "role": r}
                                       for u, i, r, _ in USERS}},
                "pdp": pdp, "claims": claims}
    path = os.path.join(EVIDENCE_DIR, "shared-identity-provisioning-evidence.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(evidence, handle, indent=2)
    print(f"evidence (sin secretos): {path}")
    if any(r["verdict"] != "PASS" for r in claims):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
