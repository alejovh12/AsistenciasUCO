"""Guardas compartidas de los E2E que modifican estado (`e2e_identity_binding.py`, `e2e_provisioning.py`).

Solo stdlib. Estos E2E cambian usuarios y vínculos de identidad, así que **solo** corren contra una base explícitamente
desechable. "Desechable" no se deduce de un nombre: son tres señales independientes que deben coincidir, y todo lo demás
se rechaza antes de tocar nada:

1. el contenedor SQL lleva la etiqueta Docker `asistencias.e2e.disposable=true` (se fija en `docker run --label`);
2. la base lleva la propiedad extendida `uco.e2e.disposable = 'true'` (se fija con `mark-disposable`, que a su vez exige 1);
3. el backend bajo prueba tiene sesiones JDBC abiertas contra ESA base (si apuntara a otra, `POST /api/v1/usuarios`
   escribiría fuera de ella).

El contenedor de desarrollo habitual (`sql_server_asistencias`, puerto host 1433, base `gestionasistenciadb`) está
bloqueado por nombre, por puerto y por nombre de base aunque alguien le pusiera la etiqueta.

La contraseña de `sa` se lee de un archivo (fuera de Git) y viaja al contenedor por variable de entorno heredada
(`docker exec -e SQLCMDPASSWORD`), nunca en la línea de comandos. Ningún valor secreto se imprime ni se guarda.

Uso para marcar una base desechable (una sola vez, tras `docker run --label asistencias.e2e.disposable=true ...`):
    python -X utf8 -I infra/security-baseline/scripts/e2e_safety.py mark-disposable --sql-container NAME --sql-password-file FILE --database DB
"""
import argparse
import json
import os
import re
import subprocess
import sys
import urllib.parse

LABEL = "asistencias.e2e.disposable"
MARKER_PROPERTY = "uco.e2e.disposable"
DEFAULT_DATABASE = "gestionasistenciadb_e2e"
DEFAULT_BACKEND = "http://127.0.0.1:8080"
PROTECTED_CONTAINERS = frozenset({"sql_server_asistencias"})
PROTECTED_HOST_PORTS = frozenset({"1433"})
PROTECTED_DATABASES = frozenset({"gestionasistenciadb"})
LOOPBACK_HOSTS = frozenset({"127.0.0.1", "localhost", "::1", "[::1]"})
GUID = re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
IDENTIFIER = re.compile(r"^[A-Za-z0-9_]{1,128}$")


class UnsafeTarget(SystemExit):
    """El destino no está identificado como desechable: el E2E se detiene sin modificar nada (código 2)."""

    def __init__(self, problems):
        self.problems = list(problems)
        super().__init__(2)


def refuse(problems):
    for problem in problems:
        print("RECHAZADO " + problem, file=sys.stderr)
    raise UnsafeTarget(problems)


def guid(value):
    """Valida un GUID antes de interpolarlo en T-SQL (los E2E no aceptan otro formato en consultas)."""
    if not isinstance(value, str) or not GUID.match(value):
        raise ValueError("se esperaba un GUID")
    return value.lower()


def identifier(value):
    if not isinstance(value, str) or not IDENTIFIER.match(value):
        raise ValueError("identificador no permitido")
    return value


def literal(value):
    """Literal T-SQL de texto: Unicode, con comillas duplicadas."""
    return "N'" + str(value).replace("'", "''") + "'"


# ---------------------------------------------------------------------------------------------------- argumentos

def parse_args(argv, description):
    parser = argparse.ArgumentParser(description=description, allow_abbrev=False)
    parser.add_argument("--sql-container", required=True)
    parser.add_argument("--sql-password-file", required=True)
    parser.add_argument("--database", default=DEFAULT_DATABASE)
    parser.add_argument("--backend", default=DEFAULT_BACKEND)
    return parser.parse_args(argv)


def read_password(path):
    with open(path, encoding="ascii") as handle:
        value = handle.read().strip()
    if not value:
        raise SystemExit("el archivo de contraseña está vacío")
    return value


# ------------------------------------------------------------------------------------------------------- acceso SQL

class Target:
    """Una base SQL Server dentro de un contenedor Docker, consultada con `sqlcmd` del propio contenedor."""

    def __init__(self, container, password, database, runner=subprocess.run):
        self.container = container
        self.database = identifier(database)
        self._password = password
        self._runner = runner

    def sql(self, query, database=None):
        env = dict(os.environ, SQLCMDPASSWORD=self._password)
        result = self._runner(
            ["docker", "exec", "-e", "SQLCMDPASSWORD", self.container, "/opt/mssql-tools18/bin/sqlcmd", "-S", "localhost",
             "-U", "sa", "-C", "-I", "-b", "-d", database or self.database, "-h", "-1", "-W", "-s", "|", "-Q",
             "SET NOCOUNT ON; " + query], capture_output=True, text=True, env=env)
        if result.returncode != 0:
            raise RuntimeError("sqlcmd falló: " + (result.stdout or result.stderr).strip()[:300])
        return result.stdout.strip()

    def rows(self, query, database=None):
        out = self.sql(query, database)
        return [line.split("|") for line in out.splitlines() if line.strip()]

    def scalar(self, query, database=None):
        found = self.rows(query, database)
        return found[0][0] if found else None


def inspect_container(name, runner=subprocess.run):
    result = runner(["docker", "inspect", name], capture_output=True, text=True)
    if result.returncode != 0:
        refuse([f"el contenedor '{name}' no existe o Docker no responde"])
    return json.loads(result.stdout)[0]


# ------------------------------------------------------------------------------------------------- evaluación de destino

def container_problems(info, database):
    """Problemas del contenedor y del nombre de base. Pura: recibe el JSON de `docker inspect`."""
    problems = []
    name = (info.get("Name") or "").lstrip("/")
    if name in PROTECTED_CONTAINERS:
        problems.append(f"'{name}' es la base de desarrollo habitual y está protegida")
    labels = (info.get("Config") or {}).get("Labels") or {}
    if labels.get(LABEL) != "true":
        problems.append(f"el contenedor no lleva la etiqueta {LABEL}=true")
    bindings = (info.get("HostConfig") or {}).get("PortBindings") or {}
    for published in bindings.values():
        for binding in published or []:
            if str(binding.get("HostPort")) in PROTECTED_HOST_PORTS:
                problems.append(f"el contenedor publica el puerto de desarrollo {binding.get('HostPort')}")
    if database in PROTECTED_DATABASES:
        problems.append(f"la base '{database}' es la de desarrollo habitual y está protegida")
    return problems


def backend_problems(backend_url, pid, sessions):
    problems = []
    host = urllib.parse.urlsplit(backend_url).hostname or ""
    if host not in LOOPBACK_HOSTS:
        problems.append(f"el backend '{host}' no es local")
    elif pid is None:
        problems.append("no se encontró el proceso que escucha en el puerto del backend")
    elif sessions < 1:
        problems.append(f"el backend (PID {pid}) no tiene sesiones abiertas contra la base desechable")
    return problems


def listener_pid(port, netstat_output):
    """PID que escucha en el puerto dado, a partir de `netstat -ano -p TCP` (en cualquier idioma de Windows)."""
    for line in (netstat_output or "").splitlines():
        parts = line.split()
        # El texto del estado depende del idioma de Windows (LISTENING / ESCUCHANDO): un socket que escucha es el que
        # no tiene dirección remota (puerto externo 0).
        if (len(parts) >= 5 and parts[0] == "TCP" and parts[1].rsplit(":", 1)[-1] == str(port)
                and parts[2].rsplit(":", 1)[-1] == "0" and parts[4].isdigit()):
            return int(parts[4])
    return None


def local_listener_pid(port, runner=subprocess.run):
    if os.name != "nt":
        return None  # solo Windows está soportado: sin PID se rechaza (cierra por defecto)
    result = runner(["netstat", "-ano", "-p", "TCP"], capture_output=True, text=True, errors="replace")
    return listener_pid(port, result.stdout) if result.returncode == 0 else None


def require_disposable(target, backend_url=DEFAULT_BACKEND, inspect=None, listener=None):
    """Detiene el E2E (UnsafeTarget) salvo que las tres señales de base desechable coincidan."""
    inspect = inspect or inspect_container
    listener = listener or local_listener_pid
    problems = container_problems(inspect(target.container), target.database)
    if problems:
        refuse(problems)  # sin etiqueta o protegida: ni siquiera se consulta la base
    marker = target.scalar(
        f"SELECT CAST(value AS NVARCHAR(20)) FROM sys.extended_properties WHERE class = 0 AND name = {literal(MARKER_PROPERTY)}")
    if marker != "true":
        problems.append(f"la base '{target.database}' no lleva la propiedad extendida {MARKER_PROPERTY}='true'")
    port = urllib.parse.urlsplit(backend_url).port or 80
    pid = listener(port)
    sessions = 0
    if pid is not None:
        sessions = int(target.scalar(
            "SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process = 1 "
            f"AND host_process_id = {int(pid)} AND database_id = DB_ID({literal(target.database)})") or 0)
    problems += backend_problems(backend_url, pid, sessions)
    if problems:
        refuse(problems)
    return target


# ------------------------------------------------------------------------------------------ estado, restauración, residuos

IDENTITY_COLUMNS = "id, usuario, emisor, sujeto, estado, CONVERT(NVARCHAR(40), fechaRevocacion, 121)"


def identity_rows(target):
    """Todas las filas de IdentidadExterna (clave `id`), sin el correo ni nada sensible."""
    return {row[0].lower(): tuple(row[1:]) for row in target.rows(f"SELECT {IDENTITY_COLUMNS} FROM dbo.IdentidadExterna")}


def user_states(target):
    return {row[0].lower(): row[1] for row in target.rows("SELECT id, CAST(estado AS INT) FROM dbo.Usuario")}


def capture_state(target):
    """Instantánea de lo que estos E2E pueden alterar: vínculos, estado de usuarios y conteo de filas por tabla."""
    return {"identities": identity_rows(target), "users": user_states(target), "counts": table_counts(target)}


def table_counts(target):
    return {row[0]: int(row[1]) for row in target.rows(
        "SELECT s.name + '.' + t.name, SUM(p.row_count) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id "
        "JOIN sys.dm_db_partition_stats p ON p.object_id = t.object_id AND p.index_id IN (0, 1) "
        "GROUP BY s.name, t.name")}


def restore_statements(before, after):
    """T-SQL que devuelve usuarios y vínculos al estado capturado en `before`; solo lo que cambió, sin valores fijos.

    Las filas de vínculo que no existían antes se eliminan; las que existían recuperan `estado` y `fechaRevocacion` tal
    como estaban (incluido NULL).
    """
    statements = []
    for user_id, state in before["users"].items():
        if after["users"].get(user_id) != state:
            statements.append(f"UPDATE dbo.Usuario SET estado = {int(state)} WHERE id = '{guid(user_id)}'")
    for row_id, (usuario, emisor, sujeto, estado, revocacion) in before["identities"].items():
        if after["identities"].get(row_id) != (usuario, emisor, sujeto, estado, revocacion):
            when = "NULL" if revocacion == "NULL" else f"CONVERT(DATETIME2, '{revocacion}', 121)"
            statements.append(f"UPDATE dbo.IdentidadExterna SET estado = {int(estado)}, fechaRevocacion = {when} "
                              f"WHERE id = '{guid(row_id)}'")
    for row_id in after["identities"]:
        if row_id not in before["identities"]:
            statements.append(f"DELETE FROM dbo.IdentidadExterna WHERE id = '{guid(row_id)}'")
    return statements


APPEND_ONLY_TABLES = frozenset({"dbo.AuditoriaEvento"})


def audit_growth(before, after, tables=APPEND_ONLY_TABLES):
    """Filas nuevas en tablas de auditoría append-only: son el rastro legítimo del backend, no residuos; se informan."""
    return {table: after["counts"].get(table, 0) - before["counts"].get(table, 0) for table in sorted(tables)
            if after["counts"].get(table, 0) > before["counts"].get(table, 0)}


def residue_report(before, after, tolerated_growth=()):
    """Diferencias entre dos instantáneas; lista vacía = estado idéntico.

    `tolerated_growth` nombra tablas append-only cuyo crecimiento no se reporta (una disminución sí).
    """
    found = []
    for table in sorted(set(before["counts"]) | set(after["counts"])):
        was, now = before["counts"].get(table), after["counts"].get(table)
        if was != now and not (table in tolerated_growth and now is not None and was is not None and now > was):
            found.append(f"tabla {table}: {was} -> {now} filas")
    if before["identities"] != after["identities"]:
        found.append("IdentidadExterna difiere de la instantánea inicial")
    if before["users"] != after["users"]:
        found.append("Usuario.estado difiere de la instantánea inicial")
    return found


def restore_and_verify(target, before, tolerated_growth=()):
    """Restaura al estado inicial y devuelve los residuos que persistan (vacío = limpio)."""
    after = capture_state(target)
    for statement in restore_statements(before, after):
        target.sql(statement)
    return residue_report(before, capture_state(target), tolerated_growth)


def referencing_columns(target):
    """(tabla, columna) de las FK que apuntan a dbo.Usuario, leídas del catálogo (no se asumen)."""
    return [(row[0], row[1]) for row in target.rows(
        "SELECT s.name + '.' + t.name, c.name FROM sys.foreign_keys fk JOIN sys.foreign_key_columns k "
        "ON k.constraint_object_id = fk.object_id JOIN sys.tables t ON t.object_id = fk.parent_object_id "
        "JOIN sys.schemas s ON s.schema_id = t.schema_id JOIN sys.columns c ON c.object_id = k.parent_object_id "
        "AND c.column_id = k.parent_column_id WHERE fk.referenced_object_id = OBJECT_ID('dbo.Usuario')")]


def delete_created_user(target, correo, existed_before):
    """Elimina el Usuario sintético creado por el E2E y sus dependientes por FK. Devuelve cuántos usuarios borró.

    Solo borra si NO existía antes de la prueba: nunca elimina datos que el E2E no creó.
    """
    if existed_before:
        return 0
    ids = [row[0] for row in target.rows(f"SELECT id FROM dbo.uv_usuario WHERE correo = {literal(correo)}")]
    for user_id in ids:
        user_id = guid(user_id)
        for table, column in referencing_columns(target):
            target.sql(f"DELETE FROM {table} WHERE [{identifier(column)}] = '{user_id}'")
        target.sql(f"DELETE FROM dbo.Usuario WHERE id = '{user_id}'")
    return len(ids)


# ------------------------------------------------------------------------------------------------------ CLI de marcado

def mark_disposable(target, inspect=None):
    """Fija la propiedad extendida en la base; exige que el contenedor ya esté etiquetado y no esté protegido."""
    problems = container_problems((inspect or inspect_container)(target.container), target.database)
    if problems:
        refuse(problems)
    exists = target.scalar(f"SELECT COUNT(*) FROM sys.extended_properties WHERE class = 0 AND name = {literal(MARKER_PROPERTY)}")
    verb = "sp_updateextendedproperty" if exists != "0" else "sp_addextendedproperty"
    target.sql(f"EXEC {verb} @name = {literal(MARKER_PROPERTY)}, @value = N'true'")


def main(argv):
    if len(argv) < 1 or argv[0] != "mark-disposable":
        raise SystemExit("uso: e2e_safety.py mark-disposable --sql-container NAME --sql-password-file FILE --database DB")
    args = parse_args(argv[1:], "Marca una base como desechable (solo contenedores etiquetados).")
    target = Target(args.sql_container, read_password(args.sql_password_file), args.database)
    mark_disposable(target)
    print(f"base '{args.database}' marcada como desechable")


if __name__ == "__main__":
    main(sys.argv[1:])
