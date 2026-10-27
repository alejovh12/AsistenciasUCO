"""E2E de la vinculación estable de identidad (SEC-BASELINE-017) contra Keycloak central + backend + SQL Server.

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_identity_binding.py --sql-container NAME --sql-password-file FILE
         [--database DB] [--backend URL]
Precondición: e2e_baseline.py ya vinculó a uco-docente. Modifica usuarios y vínculos (Keycloak y SQL), por eso SOLO corre
contra una base desechable identificada explícitamente (ver `e2e_safety.py`: etiqueta Docker + propiedad extendida de la base
+ backend conectado a ella); cualquier otro destino se rechaza antes de tocar nada. Captura el estado inicial, lo restaura
(sin valores fijos), elimina la identidad sintética y sus vínculos, y verifica que no queden residuos, también si la prueba
aborta a mitad. Sale con código 0 solo si todo pasa y el estado final es idéntico al inicial; los pasos que no pueden
ejecutarse con los datos disponibles se informan como SKIP (no cuentan como PASS).
"""
import json
import os
import secrets
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import e2e_baseline as E  # noqa: E402
import e2e_safety as S  # noqa: E402
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

PATH = "/api/v1/docente/horarios"
# Dominio que no pertenece a ninguna cuenta institucional: simula que el IdP cambió el correo de la persona.
FOREIGN_EMAIL = "correo.nuevo@otro.dominio.test"
KEYCLOAK_FIELDS = ("username", "email", "emailVerified", "enabled", "firstName", "lastName", "attributes", "requiredActions")
RESULTS = []
SKIPPED = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def skip(name, reason):
    SKIPPED.append(name)
    print(f"SKIP {name}  [{reason}]")


def keycloak_view(user):
    """Campos de la cuenta que el E2E modifica y debe restaurar (se ignoran los volátiles: timestamps, accesos, notBefore)."""
    return {field: user.get(field) for field in KEYCLOAK_FIELDS}


def active_link_count(target, email):
    return int(target.scalar(
        "SELECT COUNT(*) FROM dbo.IdentidadExterna i JOIN dbo.uv_usuario u ON u.id = i.usuario "
        f"WHERE i.estado = 1 AND LOWER(u.correo) = {S.literal(email.lower())}") or 0)


def main(argv=None):
    args = S.parse_args(argv if argv is not None else sys.argv[1:], "E2E de vinculación estable de identidad.")
    target = S.require_disposable(S.Target(args.sql_container, S.read_password(args.sql_password_file), args.database),
                                  args.backend)
    base_url = args.backend.rstrip("/")
    env = L.read_env()
    realm = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def kc_call(method, path, body=None, expect=(200, 201, 204, 404)):
        # El token de admin-cli dura ~60 s y este E2E dura más: se renueva en cada llamada. Un estado inesperado
        # (p. ej. 409 por correo duplicado, que Keycloak rechaza a propósito) detiene la prueba en vez de ignorarse.
        headers = {"Authorization": "Bearer " + L.kc_admin_token(env)}
        status, _, raw = L.http(L.plain_opener(), method, realm + path, body, headers=headers)
        if status not in expect:
            raise SystemExit(f"Keycloak {method} {path}: HTTP {status}")
        return status, (json.loads(raw) if raw else None)

    def status_for(username, password=None):
        token = spa_token.get_token(env, username, password=password)["access_token"]
        return E.request(base_url, "GET", PATH, token=token)[0]

    synthetic = "uco-e2e-sintetico-" + secrets.token_hex(4)
    synthetic_password = "Aa1!" + L.random_secret(24)  # solo en memoria: no se imprime ni se guarda

    users = kc_call("GET", "/users?username=uco-docente&exact=true")[1]
    if not users:
        raise SystemExit("uco-docente no existe en el realm; ejecute provision_asistencias.py users")
    uid, original = users[0]["id"], users[0]
    links = target.rows(f"SELECT usuario FROM dbo.IdentidadExterna WHERE sujeto = {S.literal(uid)}")
    if len(links) != 1:
        raise SystemExit("uco-docente debe tener exactamente un vínculo (ejecute e2e_baseline.py antes)")
    docente = S.guid(links[0][0])
    other_user = S.guid(target.scalar(f"SELECT TOP 1 id FROM dbo.uv_usuario WHERE id <> '{docente}' ORDER BY id"))
    if kc_call("GET", f"/users?username={synthetic}&exact=true")[1]:
        raise SystemExit("colisión improbable del nombre sintético; reintente")

    # Cuenta institucional SIN vínculo cuyo correo no tenga ya un usuario en Keycloak (Keycloak rechaza correos duplicados):
    # si el IdP la "apropiara" por correo, quedaría un vínculo nuevo que esta prueba detecta.
    unlinked_email = None
    for (candidate,) in target.rows("SELECT TOP 20 correo FROM dbo.uv_usuario u WHERE correo IS NOT NULL AND NOT EXISTS "
                                    "(SELECT 1 FROM dbo.IdentidadExterna i WHERE i.usuario = u.id) ORDER BY correo"):
        if not kc_call("GET", f"/users?email={candidate}&exact=true")[1]:
            unlinked_email = candidate
            break
    docente_email_is_linked = active_link_count(target, original.get("email") or "") == 1
    before = S.capture_state(target)

    def restore_sql():
        for statement in S.restore_statements(before, S.capture_state(target)):
            target.sql(statement)

    def links_of(user_id):
        return target.scalar(f"SELECT COUNT(*) FROM dbo.IdentidadExterna WHERE usuario = '{user_id}'")

    def links_for_subject(subject):
        return target.scalar(f"SELECT COUNT(*) FROM dbo.IdentidadExterna WHERE sujeto = {S.literal(subject)}")

    def links_to_email(email):
        return target.rows("SELECT 1 FROM dbo.IdentidadExterna i JOIN dbo.uv_usuario u ON u.id = i.usuario "
                           f"WHERE LOWER(u.correo) = {S.literal(email.lower())}")

    cleanup_problems = []
    aborted = None
    try:
        try:
            check("línea base: uco-docente autentica", status_for("uco-docente") == 200)

            # Cambio de correo en el IdP: la cuenta sigue siendo la misma (se resuelve por issuer+subject).
            kc_call("PUT", f"/users/{uid}", dict(original, email=FOREIGN_EMAIL, emailVerified=True), expect=(204,))
            check("cambio de correo en el IdP conserva la cuenta vinculada", status_for("uco-docente") == 200)

            # Correo de OTRA cuenta institucional sin vínculo: el docente sigue siendo él y esa cuenta NO se apropia.
            if unlinked_email:
                kc_call("PUT", f"/users/{uid}", dict(original, email=unlinked_email, emailVerified=True), expect=(204,))
                check("correo de otra cuenta en el IdP no apropia esa cuenta (200 con la propia, sin vínculo nuevo)",
                      status_for("uco-docente") == 200 and links_of(docente) == "1" and not links_to_email(unlinked_email))
                kc_call("PUT", f"/users/{uid}", dict(original, email=FOREIGN_EMAIL, emailVerified=True), expect=(204,))
            else:
                skip("correo de otra cuenta en el IdP no apropia esa cuenta",
                     "no hay cuenta sin vínculo con correo libre en Keycloak")

            # Correo no verificado en una identidad NUEVA (subject nuevo) nunca vincula.
            kc_call("POST", "/users", {"username": synthetic, "enabled": True, "emailVerified": False,
                                        "email": unlinked_email or "sin.verificar@otro.dominio.test", "firstName": "N",
                                        "lastName": "V", "requiredActions": [],
                                        "credentials": [{"type": "password", "value": synthetic_password, "temporary": False}]},
                    expect=(201,))
            nuevo = kc_call("GET", f"/users?username={synthetic}&exact=true")[1][0]
            check("identidad nueva con correo NO verificado -> 401", status_for(synthetic, synthetic_password) == 401)
            check("la identidad no verificada no dejó ningún vínculo", links_for_subject(nuevo["id"]) == "0")

            # Identidad nueva (subject distinto) con el correo ORIGINAL del docente, ya vinculado a otro subject: conflicto.
            # El docente lo liberó arriba (ahora usa FOREIGN_EMAIL), así que Keycloak admite el correo duplicado.
            if docente_email_is_linked:
                kc_call("PUT", f"/users/{nuevo['id']}", dict(nuevo, email=original["email"], emailVerified=True), expect=(204,))
                check("segundo subject con el correo de una cuenta ya vinculada -> 401 (sin apropiar)",
                      status_for(synthetic, synthetic_password) == 401)
                check("la cuenta docente conserva exactamente un vínculo y el sujeto nuevo no obtuvo ninguno",
                      links_of(docente) == "1" and links_for_subject(nuevo["id"]) == "0")
            else:
                skip("segundo subject con el correo de una cuenta ya vinculada",
                     "el correo del docente en Keycloak no coincide con una cuenta vinculada en SQL")

            # Revocación: el mismo token válido deja de ser aceptado y la identidad revocada no se reutiliza.
            token = spa_token.get_token(env, "uco-docente")["access_token"]
            target.sql(f"UPDATE dbo.IdentidadExterna SET estado = 0, fechaRevocacion = SYSUTCDATETIME() WHERE usuario = '{docente}'")
            check("vínculo revocado -> 401 con un JWT todavía vigente", E.request(base_url, "GET", PATH, token=token)[0] == 401)
            restore_sql()
            check("vínculo repuesto -> 200", E.request(base_url, "GET", PATH, token=token)[0] == 200)

            # Desactivación del usuario institucional: el vínculo vigente no basta.
            target.sql(f"UPDATE dbo.Usuario SET estado = 0 WHERE id = '{docente}'")
            check("usuario SQL inactivo -> 401", E.request(base_url, "GET", PATH, token=token)[0] == 401)
            restore_sql()
            check("usuario SQL reactivado -> 200", E.request(base_url, "GET", PATH, token=token)[0] == 200)

            # Claim idUsuario divergente (cuenta mal aprovisionada): se rechaza.
            # Se parte del estado actual (no de `original`): el correo de `original` puede ocuparlo aún la identidad de prueba.
            current = kc_call("GET", f"/users/{uid}")[1]
            kc_call("PUT", f"/users/{uid}", dict(current, attributes={"idUsuario": [other_user]}), expect=(204,))
            claim_status = status_for("uco-docente")
            check("claim idUsuario distinto del usuario vinculado -> 401", claim_status == 401, claim_status)
        except (SystemExit, RuntimeError) as error:
            aborted = error  # se informa DESPUÉS de limpiar: abortar nunca debe dejar el estado alterado sin avisar
            print(f"ABORTADO {error}")
    finally:
        # Primero se retira la identidad de prueba (puede ocupar el correo del docente) y después se restaura el original.
        try:
            for user in kc_call("GET", f"/users?username={synthetic}&exact=true")[1] or []:
                kc_call("DELETE", f"/users/{user['id']}")
            restore_status, _ = kc_call("PUT", f"/users/{uid}", original)
            if restore_status != 204:
                cleanup_problems.append(f"no se pudo restaurar uco-docente en Keycloak (HTTP {restore_status})")
            if keycloak_view(kc_call("GET", f"/users/{uid}")[1]) != keycloak_view(original):
                cleanup_problems.append("uco-docente en Keycloak difiere del estado inicial")
            if kc_call("GET", f"/users?username={synthetic}&exact=true")[1]:
                cleanup_problems.append("la identidad sintética sigue en Keycloak")
        except (SystemExit, RuntimeError) as error:
            cleanup_problems.append(f"limpieza de Keycloak interrumpida: {error}")
        # Los vínculos que la identidad sintética hubiera creado (sujeto nuevo) se eliminan en restore_and_verify.
        try:
            cleanup_problems += S.restore_and_verify(target, before)
        except (RuntimeError, ValueError) as error:
            cleanup_problems.append(f"limpieza SQL interrumpida: {error}")

    check("limpieza: Keycloak y SQL idénticos al estado inicial, sin identidad ni vínculos sintéticos",
          not cleanup_problems, "; ".join(cleanup_problems))
    failed = [name for name, ok in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS, {len(SKIPPED)} SKIP")
    sys.exit(1 if failed or aborted else 0)


if __name__ == "__main__":
    main()
