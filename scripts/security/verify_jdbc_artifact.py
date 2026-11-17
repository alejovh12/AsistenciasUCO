#!/usr/bin/env python3
"""SEC-002: attest that the runtime ships only the patched JDBC jar.

Accepts the Spring Boot fat jar (BOOT-INF/lib inside the archive) or the application
directory of the layered runtime image (/app, with the libraries extracted to lib/).

Trivy can normalize 13.2.1.jre11 to 13.2.1 in pom.properties.
This check does not suppress scanner findings or prove SQL/TLS connectivity.
"""

from __future__ import annotations

import argparse
import io
from pathlib import Path
from zipfile import BadZipFile, ZipFile

PATCHED_ARTIFACT = "BOOT-INF/lib/mssql-jdbc-13.2.1.jre11.jar"
PATCHED_LIBRARY = "lib/mssql-jdbc-13.2.1.jre11.jar"
POM_PROPERTIES = "META-INF/maven/com.microsoft.sqlserver/mssql-jdbc/pom.properties"


def _read_driver_metadata(runtime: Path) -> str:
    """Return pom.properties of the only JDBC driver, or raise ValueError on unexpected drivers."""
    if runtime.is_dir():
        # Layered image: every mssql-jdbc jar anywhere under the application directory counts.
        matches = sorted(path.relative_to(runtime).as_posix() for path in runtime.rglob("mssql-jdbc-*.jar"))
        if matches != [PATCHED_LIBRARY]:
            raise ValueError(f"Expected only {PATCHED_LIBRARY}; found {matches}")
        with ZipFile(runtime / PATCHED_LIBRARY) as inner:
            return inner.read(POM_PROPERTIES).decode("utf-8")
    with ZipFile(runtime) as outer:
        matches = [
            name
            for name in outer.namelist()
            if name.startswith("BOOT-INF/lib/mssql-jdbc-") and name.endswith(".jar")
        ]
        if matches != [PATCHED_ARTIFACT]:
            raise ValueError(f"Expected only {PATCHED_ARTIFACT}; found {matches}")
        with ZipFile(io.BytesIO(outer.read(PATCHED_ARTIFACT))) as inner:
            return inner.read(POM_PROPERTIES).decode("utf-8")


def attest(boot_jar: Path) -> str:
    """Inspect a Spring Boot fat jar or a layered application directory."""
    try:
        properties = _read_driver_metadata(boot_jar)
    except (BadZipFile, KeyError, OSError, UnicodeError) as error:
        raise ValueError("Unable to inspect JDBC artifact and Maven metadata") from error

    properties_by_key = dict(
        line.split("=", 1)
        for line in properties.splitlines()
        if "=" in line and not line.lstrip().startswith("#")
    )
    if properties_by_key.get("groupId") != "com.microsoft.sqlserver":
        raise ValueError("Unexpected JDBC driver groupId")
    if properties_by_key.get("artifactId") != "mssql-jdbc":
        raise ValueError("Unexpected JDBC driver artifactId")
    # Microsoft embeds its release number without the JRE suffix in some jars.
    # The patched Maven artifact has the jre11 suffix, as verified above.
    declared = properties_by_key.get("version")
    if declared not in {"13.2.1", "13.2.1.jre11"}:
        raise ValueError(f"Unexpected JDBC Maven version: {declared!r}")
    return f"Patched Microsoft JDBC artifact verified: 13.2.1.jre11 (pom.version={declared})"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Spring Boot runtime archive, or the /app directory of the layered image")
    args = parser.parse_args()
    try:
        print(attest(args.archive))
        return 0
    except ValueError as error:
        print(f"SEC-002 JDBC identity check FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
