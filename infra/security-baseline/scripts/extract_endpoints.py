"""Extrae el inventario (método, ruta) de los controladores REST del backend.

Uso: python -X utf8 -I extract_endpoints.py [--json]

Lee `src/main/java/**` y toma todo tipo anotado con `@RestController`/`@Controller`. Combina el `@RequestMapping` de la
clase con los `@Get/Post/Put/Patch/DeleteMapping` y los `@RequestMapping(method = ...)` de cada método (varios métodos
y varias rutas por anotación) y devuelve las rutas tal como las ve el PEP (`{variable}`, sin expresiones regulares).

No adivina: una ruta que no sea un literal de texto (constante, concatenación) o un `@RequestMapping` de método sin
verbo HTTP detiene la extracción con error, porque un endpoint ignorado en silencio quedaría fuera del catálogo.
La referencia autoritativa de lo que Spring MVC registra es la prueba `AccessCatalogConsistencyTest`
(`RequestMappingHandlerMapping`); este script es su espejo ejecutable sin JVM para el aprovisionamiento.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SOURCES = os.path.join(ROOT, "src", "main", "java")

MAPPING = re.compile(r"@(Get|Post|Put|Patch|Delete|Request)Mapping\b")
CONTROLLER = re.compile(r"@(?:Rest)?Controller\b")
TYPE_DECLARATION = re.compile(r"\b(?:class|record|interface)\s+\w+")
STRING = re.compile(r'"((?:\\.|[^"\\])*)"')
REGEX_VARIABLE = re.compile(r"\{([^}:]+):[^}]*}")
VERB = re.compile(r"RequestMethod\.([A-Z]+)")
STRINGS_AND_COMMENTS = re.compile(r'"(?:\\.|[^"\\])*"|/\*.*?\*/|//[^\n]*', re.S)


class ExtractionError(Exception):
    pass


def strip_comments(source):
    return STRINGS_AND_COMMENTS.sub(lambda m: m.group(0) if m.group(0).startswith('"') else " ", source)


def annotation_arguments(source, start):
    """Texto entre los paréntesis de una anotación (balanceados, respetando cadenas) o '' si no tiene."""
    index = start
    while index < len(source) and source[index].isspace():
        index += 1
    if index >= len(source) or source[index] != "(":
        return "", start
    depth, position, in_string = 0, index, False
    while position < len(source):
        char = source[position]
        if in_string:
            if char == "\\":
                position += 1
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return source[index + 1:position], position + 1
        position += 1
    raise ExtractionError("anotación sin cerrar")


def split_top_level(text):
    parts, depth, current, in_string = [], 0, [], False
    index = 0
    while index < len(text):
        char = text[index]
        if in_string:
            current.append(char)
            if char == "\\" and index + 1 < len(text):
                index += 1
                current.append(text[index])
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
            current.append(char)
        elif char in "({[":
            depth += 1
            current.append(char)
        elif char in ")}]":
            depth -= 1
            current.append(char)
        elif char == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(char)
        index += 1
    if "".join(current).strip():
        parts.append("".join(current).strip())
    return parts


def mapping_paths(args, where):
    """Rutas declaradas en `value`/`path` o en el primer argumento posicional; [''] si no hay ninguna."""
    values = []
    for part in split_top_level(args):
        match = re.match(r"(\w+)\s*=\s*(.*)$", part, re.S)
        if match and match.group(1) in ("value", "path"):
            values.append(match.group(2))
        elif not match and part:
            values.append(part)
    if not values:
        return [""]
    paths = []
    for value in values:
        literals = STRING.findall(value)
        if re.sub(r"[{},\s]", "", STRING.sub("", value)):
            raise ExtractionError(f"{where}: la ruta no es un literal de texto ({value.strip()[:60]})")
        paths.extend(literals)
    return paths or [""]


def mapping_verbs(kind, args, where):
    if kind != "Request":
        return [kind.upper()]
    verbs = []
    for part in split_top_level(args):
        match = re.match(r"method\s*=\s*(.*)$", part, re.S)
        if match:
            verbs = VERB.findall(match.group(1))
    if not verbs:
        raise ExtractionError(f"{where}: @RequestMapping de método sin verbo HTTP (aceptaría cualquiera)")
    return verbs


def join_path(prefix, suffix):
    joined = "/" + "/".join(part for part in (prefix.strip("/"), suffix.strip("/")) if part)
    return REGEX_VARIABLE.sub(r"{\1}", joined)


def file_endpoints(path):
    with open(path, encoding="utf-8") as handle:
        source = strip_comments(handle.read())
    if not CONTROLLER.search(source):
        return set()
    declaration = TYPE_DECLARATION.search(source)
    if not declaration:
        return set()
    where = os.path.relpath(path, ROOT)
    bases, found = [""], set()
    for match in MAPPING.finditer(source):
        args, _ = annotation_arguments(source, match.end())
        if match.start() < declaration.start():
            if match.group(1) == "Request":
                bases = mapping_paths(args, where)
            continue
        verbs = mapping_verbs(match.group(1), args, where)
        for base in bases:
            for suffix in mapping_paths(args, where):
                for verb in verbs:
                    found.add((verb, join_path(base, suffix)))
    return found


def endpoints():
    result = set()
    for folder, _, files in os.walk(SOURCES):
        for name in files:
            if name.endswith(".java"):
                result |= file_endpoints(os.path.join(folder, name))
    return sorted(result, key=lambda item: (item[1], item[0]))


if __name__ == "__main__":
    try:
        items = endpoints()
    except ExtractionError as error:
        raise SystemExit(f"extract_endpoints: {error}")
    if "--json" in sys.argv:
        print(json.dumps([{"method": m, "path": p} for m, p in items], indent=1))
    else:
        for method, path in items:
            print(method, path)
        print(len(items), "endpoints", file=sys.stderr)
