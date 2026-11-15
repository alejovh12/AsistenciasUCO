"""Extrae el inventario (método, ruta) de los controladores REST del backend.

Uso: python -X utf8 -I extract_endpoints.py [--json]

Lee `src/main/java/**` y toma todo tipo anotado con `@RestController`/`@Controller`. Combina el `@RequestMapping` de la
clase con los `@Get/Post/Put/Patch/DeleteMapping` y los `@RequestMapping(method = ...)` de cada método (varios métodos
y varias rutas por anotación) y devuelve las rutas tal como las ve el PEP (`{variable}`, sin expresiones regulares).

Una ruta puede ser un literal de texto o una constante `static final String` con valor literal declarada en el mismo
archivo, referida por su nombre (`RUTA`) o calificada con el tipo que la declara (`Controlador.RUTA`).
No adivina: cualquier otra ruta (constante de otra clase, concatenación, expresión) o un `@RequestMapping` de método sin
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
TYPE_DECLARATION = re.compile(r"\b(?:class|record|interface)\s+(\w+)")
CONSTANT = re.compile(r'((?:\b(?:public|protected|private|static|final)\s+)+)String\s+(\w+)\s*=\s*("(?:\\.|[^"\\])*")\s*;')
STRING_OR_NAME = re.compile(r'"(?:\\.|[^"\\])*"|(?<![\w.])[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*')
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


def string_constants(source):
    """Constantes `static final String X = "...";` del archivo, por nombre simple y calificado con su tipo."""
    types = TYPE_DECLARATION.findall(source)
    constants = {}
    for match in CONSTANT.finditer(source):
        modifiers = match.group(1).split()
        if "static" in modifiers and "final" in modifiers:
            constants[match.group(2)] = match.group(3)
            for name in types:
                constants[f"{name}.{match.group(2)}"] = match.group(3)
    return constants


def resolve_constants(value, constants):
    """Sustituye las constantes conocidas por su literal; lo demás queda igual y la validación lo rechaza."""
    return STRING_OR_NAME.sub(lambda m: constants.get(m.group(0), m.group(0)), value)


def mapping_paths(args, where, constants=None):
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
        value = resolve_constants(value, constants or {})
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
    bases, found, constants = [""], set(), string_constants(source)
    for match in MAPPING.finditer(source):
        args, _ = annotation_arguments(source, match.end())
        if match.start() < declaration.start():
            if match.group(1) == "Request":
                bases = mapping_paths(args, where, constants)
            continue
        verbs = mapping_verbs(match.group(1), args, where)
        for base in bases:
            for suffix in mapping_paths(args, where, constants):
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
