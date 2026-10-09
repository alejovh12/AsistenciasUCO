#!/usr/bin/env python3
"""SEC-002: attest that the runnable Spring Boot archive embeds the patched JDBC jar.

Trivy can normalize 13.2.1.jre11 to 13.2.1 in pom.properties.
This check does not suppress scanner findings or prove SQL/TLS connectivity.
"""

from __future__ import annotations

import argparse
import io
from pathlib import Path
from zipfile import BadZipFile, ZipFile

PATCHED_ARTIFACT = "BOOT-INF/lib/mssql-jdbc-13.2.1.jre11.jar"
POM_PROPERTIES = "META-INF/maven/com.microsoft.sqlserver/mssql-jdbc/pom.properties"


def attest(boot_jar: Path) -> str:
    """Inspect a Spring Boot fat jar and raise ValueError on unexpected drivers."""
    try:
        with ZipFile(boot_jar) as outer:
            matches = [
                name
                for name in outer.namelist()
                if name.startswith("BOOT-INF/lib/mssql-jdbc-") and name.endswith(".jar")
            ]
            if matches != [PATCHED_ARTIFACT]:
                raise ValueError(f"Expected only {PATCHED_ARTIFACT}; found {matches}")
            with ZipFile(io.BytesIO(outer.read(PATCHED_ARTIFACT))) as inner:
                properties = inner.read(POM_PROPERTIES).decode("utf-8")
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
    parser.add_argument("archive", type=Path, help="Freshly built Spring Boot runtime archive")
    args = parser.parse_args()
    try:
        print(attest(args.archive))
        return 0
    except ValueError as error:
        print(f"SEC-002 JDBC identity check FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
