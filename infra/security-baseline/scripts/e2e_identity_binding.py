"""E2E de la vinculación estable de identidad (SEC-BASELINE-017) contra Keycloak central + backend + SQL Server.

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_identity_binding.py --sql-container NAME --sql-password-file FILE
Precondición: e2e_baseline.py ya vinculó a uco-docente. Restaura todo lo que cambia (correo, estado de la cuenta,
vínculo). Sale con código 0 solo si todo pasa. Solo para bases desechables: usa `sa` del contenedor indicado para
revocar/reponer el vínculo y comprobar el estado persistido.
"""
import json
import os
import subprocess
import sys
import urllib.parse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import e2e_baseline as E  # noqa: E402
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

BASE = "http://127.0.0.1:8080"
PATH = "/api/v1/docente/horarios"
RESULTS = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def sql(container, password, query):
    out = subprocess.run(["docker", "exec", container, "/opt/mssql-tools18/bin/sqlcmd", "-S", "localhost", "-U", "sa",
                          "-P", password, "-C", "-I", "-b", "-d", "gestionasistenciadb", "-h", "-1", "-W", "-Q", "SET NOCOUNT ON; " + query],
                         capture_output=True, text=True, check=True)
    return out.stdout.strip()


def status_for(env, username):
    token = spa_token.get_token(env, username)["access_token"]
    return E.request(BASE, "GET", PATH, token=token)[0]


def main():
    container = sys.argv[sys.argv.index("--sql-container") + 1]
    password = open(sys.argv[sys.argv.index("--sql-password-file") + 1], encoding="ascii").read().strip()
    env = L.read_env()
    kc = L.kc_admin_token(env)
    headers = {"Authorization": "Bearer " + kc}
    base = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def kc_call(method, path, body=None):
        # El token de admin-cli dura ~60 s y este E2E dura más: se renueva en cada llamada y un estado inesperado
        # detiene la prueba (409 = correo duplicado, que Keycloak rechaza a propósito) en vez de ignorarse.
        fresh = {"Authorization": "Bearer " + L.kc_admin_token(env)}
        status, _, raw = L.http(L.plain_opener(), method, base + path, body, headers=fresh)
        if status not in (200, 201, 204, 404, 409):
            raise SystemExit(f"Keycloak {method} {path}: HTTP {status}")
        return status, (json.loads(raw) if raw else None)

    _, users = kc_call("GET", "/users?username=uco-docente&exact=true")
    uid, original = users[0]["id"], users[0]

    try:
        check("línea base: uco-docente autentica", status_for(env, "uco-docente") == 200)

        # Cambio de correo en el IdP: la cuenta sigue siendo la misma (se resuelve por issuer+subject).
        kc_call("PUT", f"/users/{uid}", dict(original, email="correo.nuevo@otro.dominio.test", emailVerified=True))
        check("cambio de correo en el IdP conserva la cuenta vinculada", status_for(env, "uco-docente") == 200)

        # Cambio de correo hacia el de OTRA cuenta institucional: tampoco apropia esa cuenta (sigue siendo la propia).
        kc_call("PUT", f"/users/{uid}", dict(original, email="carlos.zapata@uco.edu.co", emailVerified=True))
        check("correo de otra cuenta en el IdP no apropia esa cuenta (sigue 200 con la propia)",
              status_for(env, "uco-docente") == 200)

        # Correo no verificado en una identidad NUEVA (subject nuevo) nunca vincula.
        kc_call("POST", "/users", {"username": "uco-nuevo-noverif", "enabled": True, "emailVerified": False,
                                    "email": "ana.gomez@uco.edu.co", "firstName": "N", "lastName": "V", "requiredActions": [],
                                    "credentials": [{"type": "password", "value": env.get("LAB_USER_UCO_DOCENTE_PASSWORD"), "temporary": False}]})
        env["LAB_USER_UCO_NUEVO_NOVERIF_PASSWORD"] = env["LAB_USER_UCO_DOCENTE_PASSWORD"]
        check("identidad nueva con correo NO verificado -> 401", status_for(env, "uco-nuevo-noverif") == 401)
        _, nuevo = kc_call("GET", "/users?username=uco-nuevo-noverif&exact=true")

        # Identidad nueva (subject distinto) con el correo de una cuenta YA vinculada a otro subject: conflicto, no apropia.
        kc_call("PUT", f"/users/{nuevo[0]['id']}", dict(nuevo[0], email="maria.rostagno@uco.edu.co", emailVerified=True))
        _, a = kc_call("GET", f"/users/{nuevo[0]['id']}")
        check("segundo subject con el correo de una cuenta ya vinculada -> 401 (sin apropiar)",
              status_for(env, "uco-nuevo-noverif") == 401)
        count = sql(container, password, "SELECT COUNT(*) FROM dbo.IdentidadExterna WHERE usuario='E1F2A3B4-0000-0000-0000-000000000003'")
        check("la cuenta docente conserva exactamente un vínculo", count == "1", count)

        # Revocación: el mismo token válido deja de ser aceptado y la identidad revocada no se reutiliza.
        token = spa_token.get_token(env, "uco-docente")["access_token"]
        sql(container, password, "UPDATE dbo.IdentidadExterna SET estado=0, fechaRevocacion=SYSUTCDATETIME() WHERE usuario='E1F2A3B4-0000-0000-0000-000000000003'")
        check("vínculo revocado -> 401 con un JWT todavía vigente", E.request(BASE, "GET", PATH, token=token)[0] == 401)
        sql(container, password, "UPDATE dbo.IdentidadExterna SET estado=1, fechaRevocacion=NULL WHERE usuario='E1F2A3B4-0000-0000-0000-000000000003'")
        check("vínculo repuesto -> 200", E.request(BASE, "GET", PATH, token=token)[0] == 200)

        # Desactivación del usuario institucional: el vínculo vigente no basta.
        sql(container, password, "UPDATE dbo.Usuario SET estado=0 WHERE id='E1F2A3B4-0000-0000-0000-000000000003'")
        check("usuario SQL inactivo -> 401", E.request(BASE, "GET", PATH, token=token)[0] == 401)
        sql(container, password, "UPDATE dbo.Usuario SET estado=1 WHERE id='E1F2A3B4-0000-0000-0000-000000000003'")
        check("usuario SQL reactivado -> 200", E.request(BASE, "GET", PATH, token=token)[0] == 200)

        # Claim idUsuario divergente (cuenta mal aprovisionada): se rechaza.
        # Se parte del estado actual (no de `original`): el correo de `original` lo ocupa aún la identidad de prueba.
        _, current = kc_call("GET", f"/users/{uid}")
        kc_call("PUT", f"/users/{uid}", dict(current, attributes={"idUsuario": ["E1F2A3B4-0000-0000-0000-000000000004"]}))
        claim_status = status_for(env, "uco-docente")
        check("claim idUsuario distinto del usuario vinculado -> 401", claim_status == 401, claim_status)
    finally:
        # Primero se retira la identidad de prueba (puede ocupar el correo del docente) y después se restaura el original.
        _, extra = kc_call("GET", "/users?username=uco-nuevo-noverif&exact=true")
        for user in extra or []:
            kc_call("DELETE", f"/users/{user['id']}")
        restore_status, _ = kc_call("PUT", f"/users/{uid}", original)
        if restore_status != 204:
            raise SystemExit(f"No se pudo restaurar uco-docente (HTTP {restore_status}); revisar Keycloak")
        sql(container, password, "UPDATE dbo.Usuario SET estado=1 WHERE id='E1F2A3B4-0000-0000-0000-000000000003'; "
                                 "UPDATE dbo.IdentidadExterna SET estado=1, fechaRevocacion=NULL WHERE usuario='E1F2A3B4-0000-0000-0000-000000000003'")

    failed = [name for name, ok in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
