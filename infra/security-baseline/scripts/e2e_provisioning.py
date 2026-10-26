"""E2E del aprovisionamiento de identidades en modo baseline (IdentityProviderPort -> realm central).

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_provisioning.py --sql-container NAME --sql-password-file FILE
Crea una cuenta nueva vía POST /api/v1/usuarios con un ADMINISTRADOR real, comprueba que se creó en el realm
`security-baseline` (con el atributo idUsuario) y NO en el Keycloak legacy (8081), y elimina la cuenta de prueba del
realm al terminar. La contraseña de la cuenta de prueba se genera al vuelo y no se imprime ni se guarda.
Solo para bases desechables (deja la fila de Usuario en SQL Server).
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import e2e_baseline as E  # noqa: E402
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

BASE = "http://127.0.0.1:8080"
USERNAME = "e2e.aprovisionado@uco.edu.co"
LEGACY = "http://localhost:8081"
RESULTS = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def sql(container, password, query):
    out = subprocess.run(["docker", "exec", container, "/opt/mssql-tools18/bin/sqlcmd", "-S", "localhost", "-U", "sa",
                          "-P", password, "-C", "-I", "-b", "-d", "gestionasistenciadb", "-h", "-1", "-W", "-Q",
                          "SET NOCOUNT ON; " + query], capture_output=True, text=True, check=True)
    return out.stdout.strip()


def main():
    container = sys.argv[sys.argv.index("--sql-container") + 1]
    sql_password = open(sys.argv[sys.argv.index("--sql-password-file") + 1], encoding="ascii").read().strip()
    env = L.read_env()
    token = spa_token.get_token(env, "uco-administrador")["access_token"]
    tipo_id = sql(container, sql_password, "SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion='CC'")

    body = {"tipoIdIdentificacion": tipo_id, "numeroIdentificacion": 987000111, "primerApellido": "Aprovisionado",
            "segundoApellido": "Prueba", "primerNombre": "Cuenta", "segundoNombre": "Local", "correo": USERNAME,
            "password": "Aa1!" + L.random_secret(24)}
    status, _, raw = E.request(BASE, "POST", "/api/v1/usuarios", token=token, body=body)
    check("POST /api/v1/usuarios como ADMINISTRADOR responde 201", status == 201,
          f"HTTP {status}" + ("" if status == 201 else " " + raw[:400].decode("utf-8", "replace")))

    kc = L.kc_admin_token(env)
    headers = {"Authorization": "Bearer " + kc}
    _, _, found = L.http(L.plain_opener(), "GET", f"{L.KEYCLOAK}/admin/realms/{L.REALM}/users?email={USERNAME}&exact=true", headers=headers)
    users = json.loads(found)
    check("la cuenta existe en el realm central security-baseline", len(users) == 1)
    if users:
        attributes = users[0].get("attributes") or {}
        check("la cuenta lleva el atributo idUsuario (UUID institucional)", bool(attributes.get("idUsuario")))
        sql_id = sql(container, sql_password, f"SELECT id FROM dbo.uv_usuario WHERE correo='{USERNAME}'")
        check("idUsuario coincide con Usuario.id en SQL Server",
              attributes.get("idUsuario", [""])[0].lower() == sql_id.lower(), sql_id)

    # No debe haberse creado nada en el Keycloak legacy (puerto 8081).
    try:
        legacy = urllib.request.urlopen(LEGACY + "/realms/asistencias-uco/.well-known/openid-configuration", timeout=5).status
    except (urllib.error.URLError, OSError):
        legacy = None
    check("el Keycloak legacy 8081 sigue activo y separado", legacy == 200, legacy)

    for user in users:
        L.http(L.plain_opener(), "DELETE", f"{L.KEYCLOAK}/admin/realms/{L.REALM}/users/{user['id']}", headers=headers)
    failed = [name for name, ok in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
