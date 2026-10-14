"""SEC-BASELINE-001 — aprovisionamiento idempotente del laboratorio (usuarios, aplicación, recursos, rol).

Uso: python -I provision_lab_pdp.py

1. Crea en el realm del laboratorio tres usuarios sintéticos (contraseñas aleatorias en lab.env).
2. Inicia sesión BFF con cada uno para que el PDP aprovisione su identidad.
3. Como lab-pdp-admin registra (si falta) la aplicación asistencias-uco-lab; la credencial de un solo
   uso se guarda en lab.env y nunca se imprime.
4. Registra recursos GET de solo lectura, el rol DOCENTE_LECTURA_LAB (alcance APPLICATION), sus
   concesiones y una única asignación a lab-docente-permitido. Sin wildcard ni rol ADMIN académico.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402

APPLICATION = "asistencias-uco-lab"
ROLE = "DOCENTE_LECTURA_LAB"
USERS = [("lab-pdp-admin", "Lab", "AdminPdp"),
         ("lab-docente-permitido", "Lab", "DocentePermitido"),
         ("lab-docente-sin-permiso", "Lab", "DocenteSinPermiso")]
GRANTED = [("/api/v1/docente/horarios", "GET"), ("/api/v1/realtime/stream", "GET")]
NOT_GRANTED = [("/api/v1/estudiantes", "GET")]


def main():
    env = L.read_env()
    admin_token = L.kc_admin_token(env)
    keys = {}
    for username, first, last in USERS:
        _, keys[username] = L.ensure_lab_user(env, admin_token, username, first, last)
        _, _, status, session, _ = L.bff_session(env, username, keys[username])
        print(f"user {username}: BFF session HTTP {status}, tenant={session.get('data', {}).get('tenantId')}")

    opener, jar, _, _, _ = L.bff_session(env, "lab-pdp-admin", keys["lab-pdp-admin"])
    headers = L.xsrf_headers(jar)

    def call(method, path, body=None):
        status, _, raw = L.http(opener, method, f"{L.PDP}{path}", body, headers=headers)
        return status, (json.loads(raw) if raw else {})

    _, apps = call("GET", "/api/v1/applications?size=100")
    existing = [a for a in apps["data"]["content"] if a["name"] == APPLICATION]
    if existing:
        app_id = existing[0]["id"]
        print(f"application {APPLICATION}: exists {app_id}")
    else:
        status, created = call("POST", "/api/v1/applications", {
            "name": APPLICATION, "description": "SEC-BASELINE-001 laboratorio local AsistenciasUCO (no productivo)",
            "baseUrl": "http://127.0.0.1:8083"})
        if status != 201:
            raise SystemExit(f"register application HTTP {status} {created.get('code')}")
        app_id = created["data"]["id"]
        L.write_env_value("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL", created["data"]["credential"])
        print(f"application {APPLICATION}: created {app_id} (credential stored in lab.env)")
    L.write_env_value("LAB_PDP_APPLICATION_ID", app_id)

    _, resources = call("GET", f"/api/v1/applications/{app_id}/resources")
    by_key = {(r["path"], r["method"]): r["id"] for r in resources.get("data") or []}
    for path, method in GRANTED + NOT_GRANTED:
        if (path, method) not in by_key:
            status, body = call("POST", f"/api/v1/applications/{app_id}/resources", {"path": path, "method": method})
            if status != 201:
                raise SystemExit(f"resource {method} {path}: HTTP {status} {body.get('code')}")
            by_key[(path, method)] = body["data"]["id"]
        print(f"resource {method} {path}: {by_key[(path, method)]}")

    _, roles = call("GET", f"/api/v1/applications/{app_id}/security/roles?size=100")
    role = next((r for r in roles["data"]["content"] if r["name"] == ROLE), None)
    if role is None:
        status, body = call("POST", "/api/v1/roles", {"name": ROLE, "scope": "APPLICATION", "applicationId": app_id})
        if status != 201:
            raise SystemExit(f"role HTTP {status} {body.get('code')}")
        role_id = body["data"]["id"]
    else:
        role_id = role["id"]
    print(f"role {ROLE}: {role_id}")

    # El PDP v0.1.6 no expone GET /roles/{id}/resources; la concesión es idempotente (200 sin duplicar).
    for key in GRANTED:
        status, body = call("POST", f"/api/v1/roles/{role_id}/resources", {"roleId": role_id, "resourceId": by_key[key]})
        if status != 200:
            raise SystemExit(f"grant {key[1]} {key[0]}: HTTP {status} {body.get('code')}")
        print(f"grant {key[1]} {key[0]}: {body.get('code')}")

    _, users = call("GET", "/api/v1/users?size=100")
    allowed = next(u["id"] for u in users["data"] if u["email"].startswith("lab-docente-permitido@"))
    _, assignments = call("GET", f"/api/v1/roles/{role_id}/assignments?size=100")
    content = (assignments.get("data") or {}).get("content", [])
    if any(a.get("userId") == allowed for a in content):
        print(f"assignment {ROLE} -> lab-docente-permitido ({allowed}): exists")
    else:
        status, body = call("POST", f"/api/v1/roles/{role_id}/assignments",
                            {"roleId": role_id, "userId": allowed, "applicationId": app_id})
        print(f"assignment {ROLE} -> lab-docente-permitido ({allowed}): HTTP {status} {body.get('code')}")

    _, summary = call("GET", f"/api/v1/applications/{app_id}/security/summary")
    print("summary:", json.dumps(summary["data"]["counts"]))


if __name__ == "__main__":
    main()
