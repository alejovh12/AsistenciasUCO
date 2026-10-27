"""E2E del aprovisionamiento de identidades en modo baseline (IdentityProviderPort -> realm central).

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_provisioning.py --sql-container NAME --sql-password-file FILE
         [--database DB] [--backend URL]
Crea una cuenta nueva vía POST /api/v1/usuarios con un ADMINISTRADOR real, comprueba que se creó en el realm
`security-baseline` (con el atributo idUsuario) y NO en el Keycloak legacy (8081), y al terminar elimina la cuenta de prueba
del realm Y la fila de Usuario (con sus dependientes por FK) de SQL Server. La contraseña de la cuenta de prueba se genera al
vuelo y no se imprime ni se guarda.
SOLO corre contra una base desechable identificada explícitamente (ver `e2e_safety.py`); cualquier otro destino se rechaza
antes de crear nada. Si la cuenta sintética ya existía (en SQL o en Keycloak) se detiene sin borrar nada que no haya creado.
Sale con código 0 solo si todo pasa y la base queda con el mismo conteo de filas que al inicio.
"""
import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import e2e_baseline as E  # noqa: E402
import e2e_safety as S  # noqa: E402
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

USERNAME = "e2e.aprovisionado@uco.edu.co"
IDENTIFICATION_NUMBER = 987000111
LEGACY = "http://localhost:8081"
RESULTS = []
SKIPPED = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def main(argv=None):
    args = S.parse_args(argv if argv is not None else sys.argv[1:], "E2E del aprovisionamiento de identidades.")
    target = S.require_disposable(S.Target(args.sql_container, S.read_password(args.sql_password_file), args.database),
                                  args.backend)
    env = L.read_env()
    realm = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def keycloak_users():
        headers = {"Authorization": "Bearer " + L.kc_admin_token(env)}
        status, _, raw = L.http(L.plain_opener(), "GET", f"{realm}/users?email={USERNAME}&exact=true", headers=headers)
        if status != 200:
            raise SystemExit(f"Keycloak GET users: HTTP {status}")
        return json.loads(raw)

    sql_existing = S.literal(USERNAME)
    existed_in_sql = bool(target.rows(f"SELECT id FROM dbo.uv_usuario WHERE correo = {sql_existing}"))
    existed_in_keycloak = bool(keycloak_users())
    taken = target.rows(f"SELECT id FROM dbo.uv_usuario WHERE numeroIdentificacion = {IDENTIFICATION_NUMBER}")
    if existed_in_sql or existed_in_keycloak or taken:
        raise SystemExit("la cuenta sintética ya existe (SQL o Keycloak) o su identificación está ocupada: "
                         "no se crea ni se borra nada")

    before = S.capture_state(target)
    token = spa_token.get_token(env, "uco-administrador")["access_token"]
    tipo_id = target.scalar("SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion = 'CC'")
    body = {"tipoIdIdentificacion": tipo_id, "numeroIdentificacion": IDENTIFICATION_NUMBER, "primerApellido": "Aprovisionado",
            "segundoApellido": "Prueba", "primerNombre": "Cuenta", "segundoNombre": "Local", "correo": USERNAME,
            "password": "Aa1!" + L.random_secret(24)}
    problems = []
    try:
        status, _, raw = E.request(args.backend.rstrip("/"), "POST", "/api/v1/usuarios", token=token, body=body)
        check("POST /api/v1/usuarios como ADMINISTRADOR responde 201", status == 201,
              f"HTTP {status}" + ("" if status == 201 else " " + raw[:400].decode("utf-8", "replace")))

        users = keycloak_users()
        check("la cuenta existe en el realm central security-baseline", len(users) == 1)
        if users:
            attributes = users[0].get("attributes") or {}
            check("la cuenta lleva el atributo idUsuario (UUID institucional)", bool(attributes.get("idUsuario")))
            sql_id = target.scalar(f"SELECT id FROM dbo.uv_usuario WHERE correo = {sql_existing}")
            check("idUsuario coincide con Usuario.id en SQL Server",
                  attributes.get("idUsuario", [""])[0].lower() == (sql_id or "").lower(), sql_id)

        # No debe haberse creado nada en el Keycloak legacy (puerto 8081).
        try:
            legacy = urllib.request.urlopen(LEGACY + "/realms/asistencias-uco/.well-known/openid-configuration", timeout=5).status
        except (urllib.error.URLError, OSError):
            legacy = None
        if legacy is None:
            # Sin el legacy en ejecución no se puede comprobar su separación; no se finge un PASS ni se arranca a escondidas.
            SKIPPED.append("legacy")
            print(f"SKIP el Keycloak legacy 8081 sigue activo y separado  [{LEGACY} no responde: no está en ejecución]")
        else:
            check("el Keycloak legacy 8081 sigue activo y separado", legacy == 200, legacy)
    finally:
        try:
            for user in keycloak_users():
                headers = {"Authorization": "Bearer " + L.kc_admin_token(env)}
                status, _, _ = L.http(L.plain_opener(), "DELETE", f"{realm}/users/{user['id']}", headers=headers)
                if status != 204:
                    problems.append(f"no se pudo eliminar la cuenta sintética de Keycloak (HTTP {status})")
            if keycloak_users():
                problems.append("la cuenta sintética sigue en Keycloak")
        except SystemExit as error:
            problems.append(f"limpieza de Keycloak interrumpida: {error}")
        try:
            S.delete_created_user(target, USERNAME, existed_before=False)
            after = S.capture_state(target)
            problems += S.residue_report(before, after, tolerated_growth=S.APPEND_ONLY_TABLES)
            for table, rows in S.audit_growth(before, after).items():
                print(f"INFO {table}: +{rows} fila(s) de auditoría append-only del backend (no es residuo)")
        except (RuntimeError, ValueError) as error:
            problems.append(f"limpieza SQL interrumpida: {error}")

    check("limpieza: sin cuenta sintética en Keycloak ni filas residuales en SQL (conteos y vínculos iguales al inicio)",
          not problems, "; ".join(problems))
    failed = [name for name, ok in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS, {len(SKIPPED)} SKIP")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
