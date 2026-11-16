"""VAL PRU-09 (lado backend): matriz de trazabilidad Historia de Usuario -> pruebas del backend.

Uso (desde la raiz del repositorio):
    python -X utf8 -I scripts/testing/matriz_hu_pruebas.py           compuerta: falla (exit 1) si la matriz versionada no
                                                                     coincide con las pruebas actuales
    python -X utf8 -I scripts/testing/matriz_hu_pruebas.py --write   regenera docs/testing/MATRIZ_HU_PRUEBAS_BACKEND.md

Una prueba cuenta para una HU cuando su archivo (src/test/java/**/*.java) cita el codigo HU### en un nombre, un @Tag, un
@DisplayName o un comentario. La capa sale de la ruta o del nombre: arquitectura (paquete architecture), integracion (*IT),
contrato (paquete openapi o *Contract*), web (WebMvc, Http, controller) o unitaria. Los casos son las anotaciones @Test,
@ParameterizedTest y @RepeatedTest del archivo. Solo usa la biblioteca estandar.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[2]
PRUEBAS = RAIZ / "src" / "test" / "java"
MATRIZ = RAIZ / "docs" / "testing" / "MATRIZ_HU_PRUEBAS_BACKEND.md"
HU = re.compile(r"\bHU(\d{3}[A-Z]?)\b")
CASO = re.compile(r"^\s*@(Test|ParameterizedTest|RepeatedTest)\b", re.MULTILINE)
ORDEN_CAPAS = ("unitaria", "web", "contrato", "integracion", "arquitectura")


def capa(relativa: str) -> str:
    nombre = relativa.rsplit("/", 1)[-1]
    if "/architecture/" in relativa:
        return "arquitectura"
    if nombre.endswith("IT.java"):
        return "integracion"
    if "/openapi/" in relativa or "Contract" in nombre:
        return "contrato"
    if "WebMvc" in nombre or "Http" in nombre or "/controller/" in relativa:
        return "web"
    return "unitaria"


def recolectar():
    por_hu = {}
    con_hu = 0
    total = 0
    for archivo in sorted(PRUEBAS.rglob("*.java")):
        texto = archivo.read_text(encoding="utf-8")
        casos = len(CASO.findall(texto))
        if casos == 0:
            continue
        total += 1
        relativa = archivo.relative_to(RAIZ).as_posix()
        codigos = sorted({"HU" + m for m in HU.findall(texto)})
        if codigos:
            con_hu += 1
        for codigo in codigos:
            por_hu.setdefault(codigo, []).append((relativa, casos, capa(relativa)))
    return por_hu, con_hu, total


def clave_hu(codigo: str):
    numero = re.match(r"HU(\d{3})([A-Z]?)", codigo)
    return int(numero.group(1)), numero.group(2)


def generar() -> str:
    por_hu, con_hu, total = recolectar()
    lineas = [
        "# Matriz HU → pruebas del backend (VAL PRU-09)",
        "",
        "Generada por `scripts/testing/matriz_hu_pruebas.py --write`; no editar a mano. El CI (`Backend Quality Gate`) falla si",
        "queda desactualizada. Una prueba cuenta para una HU cuando su archivo cita el código HU en un nombre, un `@Tag`, un",
        "`@DisplayName` o un comentario; el número entre paréntesis es la cantidad de casos (`@Test`, `@ParameterizedTest`,",
        "`@RepeatedTest`) del archivo. Es la columna \"Pruebas (backend)\" de la HU-CATALOG",
        "(`AsistenciasUCO-Infra/docs/user-stories/HU-CATALOG.md`); la del frontend está en su repositorio.",
        "",
        f"Resumen: {len(por_hu)} HU con pruebas en {con_hu} de {total} archivos de prueba.",
        "",
        "| HU | Capas | Archivos | Casos | Pruebas |",
        "|---|---|---|---|---|",
    ]
    for codigo in sorted(por_hu, key=clave_hu):
        filas = por_hu[codigo]
        capas = ", ".join(c for c in ORDEN_CAPAS if any(f[2] == c for f in filas))
        casos = sum(f[1] for f in filas)
        pruebas = "<br>".join(f"`{ruta}` ({n})" for ruta, n, _ in filas)
        lineas.append(f"| {codigo} | {capas} | {len(filas)} | {casos} | {pruebas} |")
    return "\n".join(lineas) + "\n"


def main() -> int:
    contenido = generar()
    if "--write" in sys.argv:
        MATRIZ.write_text(contenido, encoding="utf-8", newline="\n")
        print(f"MATRIZ_HU_PRUEBAS_ESCRITA {MATRIZ.relative_to(RAIZ).as_posix()}")
        return 0
    actual = MATRIZ.read_text(encoding="utf-8") if MATRIZ.exists() else ""
    if actual.replace("\r\n", "\n") != contenido:
        print("MATRIZ_HU_PRUEBAS_DESACTUALIZADA: ejecute python -X utf8 -I scripts/testing/matriz_hu_pruebas.py --write")
        return 1
    resumen = next(l for l in contenido.splitlines() if l.startswith("Resumen:"))
    print("MATRIZ_HU_PRUEBAS_PASS " + resumen)
    return 0


if __name__ == "__main__":
    sys.exit(main())
