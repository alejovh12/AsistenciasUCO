"""Aprovisionamiento idempotente de AsistenciasUCO en Security Baseline 0.1.7.

Uso (desde la raíz del repo, con la instalación de infra/security-baseline arriba):
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py keycloak
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py pdp
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py users

Lee y escribe %USERPROFILE%\\.asistencias-security-lab\\lab.env (fuera de Git). Nunca imprime contraseñas, secretos de
cliente, credenciales de aplicación ni tokens.

keycloak  Realm central: cliente de recurso `asistencias-api` (cinco roles), cliente público PKCE `asistencias-uco-spa`
          (audiencias, roles, idUsuario, email_verified) y cliente de servicio `asistencias-provisioner` (Admin REST
          mínimo para el IdentityProviderPort del modo baseline).
pdp       PDP: aplicación `asistencias-uco`, catálogo de recursos (rutas de los controladores), los cinco roles y sus
          concesiones (misma matriz que SecurityConfig).
users     Usuarios de prueba por rol (correo verificado = correo de las semillas SQL, atributo idUsuario), sesión BFF
          para aprovisionar su identidad en el PDP y asignación del rol en la aplicación.
"""
import json
import os
import re
import sys
import urllib.parse

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import extract_endpoints  # noqa: E402
import lab_pdp as L  # noqa: E402

APP_NAME = "asistencias-uco"
API_CLIENT = "asistencias-api"
SPA_CLIENT = "asistencias-uco-spa"
PROVISIONER = "asistencias-provisioner"
SPA_ORIGINS = ["http://localhost:4200", "http://127.0.0.1:4200"]
ROLES = ["ADMINISTRADOR", "DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE"]
ALL = tuple(ROLES)

# Usuarios de prueba: correo y Usuario.id de las semillas de gestion-asistencia-db (schema/seed/12_usuarios_roles.sql).
TEST_USERS = {
    "ADMINISTRADOR": ("uco-administrador", "admin.institucional@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000000"),
    "DECANO": ("uco-decano", "decano.ingenieria@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000001"),
    "COORDINADOR": ("uco-coordinador", "coordinador.sistemas@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000002"),
    "DOCENTE": ("uco-docente", "maria.rostagno@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000003"),
    "ESTUDIANTE": ("uco-estudiante", "carlos.zapata@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000004"),
}


# ── Matriz de roles: réplica ordenada (primer match) de SecurityConfig.securityFilterChain ──────────────────────
# (métodos | None = cualquiera, patrón estilo Ant, roles). `*`/`{x}` = un segmento, `**` = cualquier sufijo.
def _rule(methods, patterns, roles):
    return (methods, patterns if isinstance(patterns, (list, tuple)) else [patterns], roles)


RULES = [
    _rule({"GET"}, ["/api/v1/admin/sedes", "/api/v1/admin/sedes/**", "/api/v1/admin/areas", "/api/v1/admin/areas/**"],
          ("ADMINISTRADOR", "DECANO", "COORDINADOR")),
    _rule(None, "/api/v1/admin/**", ("ADMINISTRADOR",)),
    _rule(None, "/api/v1/decano/**", ("DECANO", "ADMINISTRADOR")),
    _rule({"GET"}, ["/api/v1/coordinador/planes-estudio", "/api/v1/coordinador/planes-estudio/**",
                    "/api/v1/coordinador/periodos-academicos", "/api/v1/coordinador/periodos-academicos/**",
                    "/api/v1/coordinador/asignaturas", "/api/v1/coordinador/asignaturas/**"],
          ("DECANO", "COORDINADOR", "ADMINISTRADOR")),
    _rule(None, "/api/v1/coordinador/**", ("COORDINADOR", "ADMINISTRADOR")),
    _rule(None, "/api/v1/docente/**", ("DOCENTE",)),
    _rule(None, "/api/v1/docentes/**", ("COORDINADOR", "ADMINISTRADOR")),
    _rule(None, "/api/v1/estudiante/**", ("ESTUDIANTE",)),
    _rule(None, "/api/v1/estudiantes/**", ("DOCENTE", "COORDINADOR", "ADMINISTRADOR")),
    _rule({"POST"}, "/api/v1/usuarios", ALL),
    _rule({"POST", "PUT", "PATCH"}, "/api/v1/sesiones/**", ("DOCENTE",)),
    _rule({"GET"}, "/api/v1/sesiones/*/qr-token", ("DOCENTE",)),
    _rule({"GET"}, "/api/v1/sesiones/grupo/*", ("DOCENTE",)),
    _rule(None, "/api/v2/sesiones/**", ("DOCENTE",)),
    _rule({"POST"}, "/api/v1/asistencias/lote", ("DOCENTE",)),
    _rule({"POST"}, "/api/v1/asistencias/revisiones", ("ESTUDIANTE",)),
    _rule({"POST"}, "/api/v1/archivos/subir", ("ESTUDIANTE",)),
    _rule({"POST"}, "/api/v1/asistencias", ("DOCENTE",)),
    _rule({"POST"}, "/api/v1/asistencias/consultas/grupo", ("DOCENTE", "COORDINADOR", "ADMINISTRADOR")),
    _rule({"GET"}, "/api/v1/grupos", ("COORDINADOR", "ADMINISTRADOR")),
    _rule({"GET"}, "/api/v1/grupos/**", ("DOCENTE", "COORDINADOR", "ADMINISTRADOR")),
    _rule({"POST"}, "/api/v1/grupos/*/estudiantes", ("DOCENTE", "COORDINADOR")),
    _rule({"POST", "PUT", "DELETE"}, "/api/v1/grupos/**", ("COORDINADOR", "ADMINISTRADOR")),
    _rule({"POST"}, "/api/v1/realtime/emit", ("ADMINISTRADOR",)),
    _rule(None, "/api/v1/realtime/**", ALL),
    _rule(None, "/api/v1/**", ALL),
]
EXEMPT = {("POST", "/api/v1/internal/azure-events")}  # webhook con su propia autenticación; fuera del PEP


def ant_regex(pattern):
    if pattern.endswith("/**"):  # como PathPattern de Spring: `/a/**` también coincide con `/a`
        return re.compile("^" + ant_regex(pattern[:-3]).pattern[1:-1] + "(/.*)?$")
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**", i):
            out += ".*"
            i += 2
        elif pattern[i] == "*":
            out += "[^/]*"
            i += 1
        else:
            out += re.escape(pattern[i])
            i += 1
    return re.compile("^" + out + "$")


def roles_for(method, path):
    concrete = re.sub(r"\{[^}]+\}", "x", path)
    for methods, patterns, roles in RULES:
        if (methods is None or method in methods) and any(ant_regex(p).match(concrete) for p in patterns):
            return roles
    raise SystemExit(f"Sin regla para {method} {path}")


# ── Keycloak ────────────────────────────────────────────────────────────────────────────────────────────────────
class Keycloak:
    def __init__(self, env):
        self.env = env
        self.token = L.kc_admin_token(env)
        self.base = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def call(self, method, path, body=None, expect=(200, 201, 204)):
        status, headers, raw = L.http(L.plain_opener(), method, self.base + path, body,
                                      headers={"Authorization": "Bearer " + self.token})
        if status not in expect:
            raise SystemExit(f"Keycloak {method} {path}: HTTP {status}")
        return status, headers, (json.loads(raw) if raw else None)

    def client(self, client_id):
        _, _, found = self.call("GET", "/clients?clientId=" + urllib.parse.quote(client_id))
        return found[0] if found else None

    def ensure_client(self, representation):
        existing = self.client(representation["clientId"])
        if existing:
            merged = dict(existing)
            merged.update({k: v for k, v in representation.items() if k != "protocolMappers"})
            self.call("PUT", f"/clients/{existing['id']}", merged)
            known = {m["name"]: m for m in existing.get("protocolMappers", [])}
            for mapper in representation.get("protocolMappers", []):
                if mapper["name"] in known:
                    self.call("PUT", f"/clients/{existing['id']}/protocol-mappers/models/{known[mapper['name']]['id']}",
                              dict(mapper, id=known[mapper["name"]]["id"]))
                else:
                    self.call("POST", f"/clients/{existing['id']}/protocol-mappers/models", mapper)
            return existing["id"], False
        self.call("POST", "/clients", representation)
        return self.client(representation["clientId"])["id"], True


def provision_keycloak(env):
    kc = Keycloak(env)
    evidence = {}

    # 1. Cliente de recurso con los cinco roles institucionales.
    api = kc.client(API_CLIENT)
    if not api:
        kc.call("POST", "/clients", {"clientId": API_CLIENT, "enabled": True, "bearerOnly": True,
                                     "publicClient": False, "protocol": "openid-connect"})
        api = kc.client(API_CLIENT)
    _, _, existing_roles = kc.call("GET", f"/clients/{api['id']}/roles")
    have = {r["name"] for r in existing_roles}
    for role in ROLES:
        if role not in have:
            kc.call("POST", f"/clients/{api['id']}/roles", {"name": role})
    evidence["api_roles"] = ROLES

    # 2. Cliente público Authorization Code + PKCE (la SPA nunca recibe secretos ni usa password grant).
    spa = {
        "clientId": SPA_CLIENT, "name": "AsistenciasUCO SPA", "protocol": "openid-connect", "enabled": True,
        "publicClient": True, "standardFlowEnabled": True, "implicitFlowEnabled": False,
        "directAccessGrantsEnabled": False, "serviceAccountsEnabled": False, "fullScopeAllowed": False,
        "redirectUris": [o + "/*" for o in SPA_ORIGINS], "webOrigins": SPA_ORIGINS,
        "attributes": {
            "pkce.code.challenge.method": "S256", "post.logout.redirect.uris": "##".join(o + "/*" for o in SPA_ORIGINS),
            "access.token.lifespan": "300",
        },
        "defaultClientScopes": ["security-baseline-claims", "email", "profile"],
        "protocolMappers": [
            _mapper("audience-asistencias-api", "oidc-audience-mapper", {"included.client.audience": API_CLIENT}),
            _mapper("idUsuario", "oidc-usermodel-attribute-mapper", {
                "user.attribute": "idUsuario", "claim.name": "idUsuario", "jsonType.label": "String", "multivalued": "false"}),
            _mapper("email_verified", "oidc-usermodel-property-mapper", {
                "user.attribute": "emailVerified", "claim.name": "email_verified", "jsonType.label": "boolean"}),
            _mapper("asistencias-api-client-roles", "oidc-usermodel-client-role-mapper", {
                "usermodel.clientRoleMapping.clientId": API_CLIENT, "claim.name": "resource_access.${client_id}.roles",
                "jsonType.label": "String", "multivalued": "true"}),
        ],
    }
    spa_id, created = kc.ensure_client(spa)
    _ensure_default_scopes(kc, spa_id, spa["defaultClientScopes"])
    # Con fullScopeAllowed=false el mapper solo emite los roles que están en el scope del cliente.
    _, _, api_role_reps = kc.call("GET", f"/clients/{api['id']}/roles")
    kc.call("POST", f"/clients/{spa_id}/scope-mappings/clients/{api['id']}", api_role_reps)
    evidence["spa_client"] = {"created": created, "pkce": "S256", "origins": SPA_ORIGINS}

    # 3. Cliente de servicio para el aprovisionamiento del backend (Admin REST mínimo).
    prov = {"clientId": PROVISIONER, "name": "AsistenciasUCO provisioner", "protocol": "openid-connect", "enabled": True,
            "publicClient": False, "serviceAccountsEnabled": True, "standardFlowEnabled": False,
            "directAccessGrantsEnabled": False, "fullScopeAllowed": False, "clientAuthenticatorType": "client-secret"}
    prov_id, created = kc.ensure_client(prov)
    _, _, svc_user = kc.call("GET", f"/clients/{prov_id}/service-account-user")
    rm = kc.client("realm-management")
    _, _, rm_roles = kc.call("GET", f"/clients/{rm['id']}/roles")
    wanted = [r for r in rm_roles if r["name"] in ("manage-users", "view-users", "query-users", "view-clients", "query-clients")]
    kc.call("POST", f"/users/{svc_user['id']}/role-mappings/clients/{rm['id']}", wanted)
    _, _, secret = kc.call("GET", f"/clients/{prov_id}/client-secret")
    L.write_env_value("SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET", secret["value"])
    evidence["provisioner"] = {"created": created, "realm_management_roles": [r["name"] for r in wanted],
                               "secret": "stored in lab.env"}
    return evidence


def _mapper(name, kind, config):
    base = {"id.token.claim": "false", "access.token.claim": "true", "introspection.token.claim": "true",
            "userinfo.token.claim": "false"}
    base.update(config)
    return {"name": name, "protocol": "openid-connect", "protocolMapper": kind, "consentRequired": False, "config": base}


def _ensure_default_scopes(kc, client_uuid, names):
    _, _, scopes = kc.call("GET", "/client-scopes")
    by_name = {s["name"]: s["id"] for s in scopes}
    _, _, current = kc.call("GET", f"/clients/{client_uuid}/default-client-scopes")
    have = {s["name"] for s in current}
    for name in names:
        if name not in have and name in by_name:
            kc.call("PUT", f"/clients/{client_uuid}/default-client-scopes/{by_name[name]}")


# ── PDP ─────────────────────────────────────────────────────────────────────────────────────────────────────────
class Pdp:
    def __init__(self, env):
        self.env = env
        self.opener, self.jar, status, _, _ = L.bff_session(env, "lab-pdp-admin", "LAB_USER_LAB_PDP_ADMIN_PASSWORD")
        if status != 200:
            raise SystemExit(f"Sesión BFF del administrador PDP: HTTP {status}")
        self.headers = L.xsrf_headers(self.jar)

    def call(self, method, path, body=None):
        status, _, raw = L.http(self.opener, method, L.PDP + path, body, headers=self.headers)
        try:
            return status, (json.loads(raw) if raw else {})
        except ValueError:
            return status, {}


def provision_pdp(env):
    pdp = Pdp(env)
    evidence = {}
    _, apps = pdp.call("GET", "/api/v1/applications?size=100")
    found = [a for a in apps["data"]["content"] if a["name"] == APP_NAME]
    if found:
        app_id = found[0]["id"]
        evidence["application"] = {"id": app_id, "created": False}
        if not env.get("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS"):
            status, body = pdp.call("POST", f"/api/v1/applications/{app_id}/credential-rotations")
            if status not in (200, 201):
                raise SystemExit(f"rotación de credencial: HTTP {status} {body.get('code')}")
            L.write_env_value("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS", body["data"]["credential"])
    else:
        status, body = pdp.call("POST", "/api/v1/applications", {
            "name": APP_NAME, "description": "AsistenciasUCO backend (Security Baseline 0.1.7)",
            "baseUrl": "http://127.0.0.1:8080"})
        if status != 201:
            raise SystemExit(f"registrar aplicación: HTTP {status} {body.get('code')}")
        app_id = body["data"]["id"]
        L.write_env_value("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS", body["data"]["credential"])
        evidence["application"] = {"id": app_id, "created": True}
    L.write_env_value("ASISTENCIAS_PDP_APPLICATION_ID", app_id)

    # Recursos: un registro por (método, ruta) del catálogo de controladores.
    catalog = [(m, p) for m, p in extract_endpoints.endpoints() if (m, p) not in EXEMPT]
    _, res = pdp.call("GET", f"/api/v1/applications/{app_id}/resources")
    by_key = {(r["method"], r["path"]): r["id"] for r in (res.get("data") or [])}
    created = 0
    for method, path in catalog:
        if (method, path) not in by_key:
            status, body = pdp.call("POST", f"/api/v1/applications/{app_id}/resources", {"path": path, "method": method})
            if status != 201:
                raise SystemExit(f"recurso {method} {path}: HTTP {status} {body.get('code')}")
            by_key[(method, path)] = body["data"]["id"]
            created += 1
    evidence["resources"] = {"catalog": len(catalog), "created": created}

    # Roles y concesiones (idempotentes).
    _, roles = pdp.call("GET", f"/api/v1/applications/{app_id}/security/roles?size=100")
    role_ids = {r["name"]: r["id"] for r in roles["data"]["content"]}
    for name in ROLES:
        if name not in role_ids:
            status, body = pdp.call("POST", "/api/v1/roles", {"name": name, "scope": "APPLICATION", "applicationId": app_id})
            if status != 201:
                raise SystemExit(f"rol {name}: HTTP {status} {body.get('code')}")
            role_ids[name] = body["data"]["id"]
    grants = {name: 0 for name in ROLES}
    for method, path in catalog:
        for role in roles_for(method, path):
            status, body = pdp.call("POST", f"/api/v1/roles/{role_ids[role]}/resources",
                                    {"roleId": role_ids[role], "resourceId": by_key[(method, path)]})
            if status != 200:
                raise SystemExit(f"concesión {role} {method} {path}: HTTP {status} {body.get('code')}")
            grants[role] += 1
    evidence["grants"] = grants
    for name, role_id in role_ids.items():
        L.write_env_value("ASISTENCIAS_PDP_ROLE_" + name + "_ID", role_id)
    return evidence


# ── Usuarios de prueba ──────────────────────────────────────────────────────────────────────────────────────────
def provision_users(env):
    kc = Keycloak(env)
    api = kc.client(API_CLIENT)
    _, _, api_roles = kc.call("GET", f"/clients/{api['id']}/roles")
    role_rep = {r["name"]: r for r in api_roles}
    evidence = {}
    for role, (username, email, user_uuid) in TEST_USERS.items():
        key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
        _, _, users = kc.call("GET", f"/users?username={username}&exact=true")
        if not users:
            kc.call("POST", "/users", {
                "username": username, "enabled": True, "emailVerified": True, "email": email,
                "firstName": role.title(), "lastName": "UCO", "requiredActions": [],
                "attributes": {"idUsuario": [user_uuid.lower()]}})
            _, _, users = kc.call("GET", f"/users?username={username}&exact=true")
        uid = users[0]["id"]
        password = env.get(key) or L.random_secret()
        if not env.get(key):
            L.write_env_value(key, password)
            env[key] = password
        kc.call("PUT", f"/users/{uid}/reset-password", {"type": "password", "value": password, "temporary": False})
        kc.call("POST", f"/users/{uid}/role-mappings/clients/{api['id']}", [role_rep[role]])
        evidence[role] = {"username": username, "client_role": role}

    pdp = Pdp(env)
    app_id = env["ASISTENCIAS_PDP_APPLICATION_ID"]
    for role, (username, email, _) in TEST_USERS.items():
        key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
        _, _, status, session, _ = L.bff_session(env, username, key)  # aprovisiona la identidad en el PDP
        evidence[role]["pdp_session_http"] = status
    _, users = pdp.call("GET", "/api/v1/users?size=100")
    pdp_ids = {u["email"].lower(): u["id"] for u in users["data"]}
    for role, (username, email, _) in TEST_USERS.items():
        role_id = env["ASISTENCIAS_PDP_ROLE_" + role + "_ID"]
        user_id = pdp_ids.get(email.lower())
        if not user_id:
            raise SystemExit(f"{username}: el PDP no conoce al usuario tras la sesión BFF")
        _, current = pdp.call("GET", f"/api/v1/roles/{role_id}/assignments?size=100")
        if any(a.get("userId") == user_id for a in (current.get("data") or {}).get("content", [])):
            evidence[role]["assignment"] = "exists"
            continue
        status, body = pdp.call("POST", f"/api/v1/roles/{role_id}/assignments",
                                {"roleId": role_id, "userId": user_id, "applicationId": app_id})
        evidence[role]["assignment"] = f"HTTP {status}"
    return evidence


def main():
    if len(sys.argv) < 2 or sys.argv[1] not in ("keycloak", "pdp", "users"):
        raise SystemExit(__doc__)
    env = L.read_env()
    stage = {"keycloak": provision_keycloak, "pdp": provision_pdp, "users": provision_users}[sys.argv[1]]
    print(json.dumps(stage(env), indent=1))


if __name__ == "__main__":
    main()
