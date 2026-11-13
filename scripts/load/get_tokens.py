"""VAL-oleada3 (REA-01/REA-06): obtiene access tokens de los usuarios de prueba del lab para la prueba de carga k6.

Reutiliza infra/security-baseline/scripts/spa_token.py (Authorization Code + PKCE, igual que la SPA; sin password grant).
Las contraseñas se leen de %USERPROFILE%\\.asistencias-security-lab\\lab.env (fuera de Git) y NUNCA se imprimen.
Los tokens se escriben en el archivo --out (JSON {ROL: token}); el llamador lo monta en el contenedor k6 y lo borra al
terminar. Por consola solo sale "OK <ROL>" o el error sin valores sensibles.

Uso: py -3 -X utf8 -I scripts/load/get_tokens.py --out <ruta.json> [--roles ESTUDIANTE,DOCENTE,COORDINADOR]
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "infra", "security-baseline", "scripts"))
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

USERS = {
    "ADMINISTRADOR": "uco-administrador",
    "DECANO": "uco-decano",
    "COORDINADOR": "uco-coordinador",
    "DOCENTE": "uco-docente",
    "ESTUDIANTE": "uco-estudiante",
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--roles", default="ESTUDIANTE,DOCENTE,COORDINADOR")
    args = parser.parse_args()
    env = L.read_env()
    tokens = {}
    for role in [r.strip().upper() for r in args.roles.split(",") if r.strip()]:
        if role not in USERS:
            raise SystemExit(f"rol desconocido: {role}")
        tokens[role] = spa_token.get_token(env, USERS[role])["access_token"]
        print(f"OK {role}")
    with open(args.out, "w", encoding="ascii") as handle:
        json.dump(tokens, handle)
    return 0


if __name__ == "__main__":
    sys.exit(main())
