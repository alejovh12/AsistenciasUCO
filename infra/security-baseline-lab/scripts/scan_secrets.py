"""SEC-BASELINE-003 — busca VALORES secretos locales dentro de rutas del repositorio, sin imprimirlos.

Uso: python -I scan_secrets.py <ruta> [<ruta> ...]

Lee los valores (no las claves) de los archivos de secretos locales del laboratorio, del .env del
backend y del .env institucional, y comprueba que ninguno aparece en los archivos indicados. Solo
imprime el nombre de la VARIABLE y el archivo donde aparecería. Exit 0 = 0 coincidencias.
"""
import os
import sys

HOME = os.path.expanduser("~")
SOURCES = [
    os.path.join(HOME, ".asistencias-security-lab", "lab.env"),
    os.path.join(HOME, ".asistencias-security-lab", "e2e-sql.env"),
    os.path.join(HOME, "AsistenciasUCO", "AsistenciasUCO", ".env"),
    os.path.join(HOME, "AsistenciasUCO", "AsistenciasUCO", "infra", "keycloak", ".env"),
]
SENSITIVE = {"PASSWORD", "SECRET", "CREDENTIAL", "TOKEN", "KEY", "PWD"}


def secret_values():
    values = {}
    for path in SOURCES:
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8-sig") as handle:
            for line in handle:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                value = value.strip().strip('"')
                if SENSITIVE & set(key.strip().upper().split("_")) and len(value) >= 8:
                    values[value] = f"{os.path.basename(path)}:{key.strip()}"
    return values


def files(paths):
    for root in paths:
        if os.path.isfile(root):
            yield root
            continue
        for directory, _, names in os.walk(root):
            for name in names:
                yield os.path.join(directory, name)


def main(paths):
    values = secret_values()
    hits = 0
    scanned = 0
    for path in files(paths):
        try:
            with open(path, encoding="utf-8", errors="ignore") as handle:
                content = handle.read()
        except OSError:
            continue
        scanned += 1
        for value, origin in values.items():
            if value in content:
                hits += 1
                print(f"SECURITY_FINDING: valor de {origin} presente en {path}")
    print(f"secretos comprobados={len(values)} archivos={scanned} coincidencias={hits}")
    return 0 if hits == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:] or ["."]))
