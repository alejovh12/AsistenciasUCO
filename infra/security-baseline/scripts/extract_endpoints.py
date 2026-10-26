"""Extrae el catálogo (método, ruta) de los controladores REST del backend.

Uso: python -X utf8 -I extract_endpoints.py [--json]

Lee `src/main/java/**/controller/**/*Controller.java`, combina el `@RequestMapping` de la clase con los
`@Get/Post/Put/Patch/DeleteMapping` de cada método y devuelve las rutas tal como las ve el PEP (con `{variable}`).
Es la fuente del catálogo de recursos que `provision_asistencias.py` registra en el PDP.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
CONTROLLERS = os.path.join(ROOT, "src", "main", "java")

MAPPING = re.compile(r"@(Get|Post|Put|Patch|Delete|Request)Mapping\s*(\((?P<args>[^)]*)\))?")
STRING = re.compile(r'"([^"]*)"')


def paths_in(args):
    if not args:
        return [""]
    value = re.search(r"(?:value|path)\s*=\s*(\{[^}]*\}|\"[^\"]*\")", args)
    text = value.group(1) if value else (args if "=" not in args.split(",")[0] else "")
    found = STRING.findall(text)
    return found or [""]


def endpoints():
    result = set()
    for folder, _, files in os.walk(CONTROLLERS):
        for name in files:
            if not name.endswith("Controller.java"):
                continue
            source = open(os.path.join(folder, name), encoding="utf-8").read()
            class_match = re.search(r"\bclass\s+\w+", source)
            if not class_match:
                continue
            head, body = source[: class_match.start()], source[class_match.start():]
            base = [""]
            for m in MAPPING.finditer(head):
                if m.group(1) == "Request":
                    base = paths_in(m.group("args"))
            for m in MAPPING.finditer(body):
                kind = m.group(1)
                if kind == "Request":
                    continue
                for prefix in base:
                    for suffix in paths_in(m.group("args")):
                        path = ("/" + (prefix + suffix).strip("/")).replace("//", "/") if (prefix + suffix) else "/"
                        result.add((kind.upper(), path))
    return sorted(result, key=lambda item: (item[1], item[0]))


if __name__ == "__main__":
    items = endpoints()
    if "--json" in sys.argv:
        print(json.dumps([{"method": m, "path": p} for m, p in items], indent=1))
    else:
        for method, path in items:
            print(method, path)
        print(len(items), "endpoints", file=sys.stderr)
