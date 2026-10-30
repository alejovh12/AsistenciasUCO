---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-29
---

# PLAN — Cruce de horario del docente (opción A)

## Decisión

John (2026-10-29): **un docente no puede tener dos grupos a la misma hora.** En la BD (rama `fix/cruce-horario-docente` del repo de BD), `usp_actualizar_grupo` rechaza el cambio de titular cuando los horarios del grupo se traslapan con otro grupo del nuevo docente en el mismo período. La respuesta es `DBCODE=HOR_002`.

## Cambio en el backend

- **`DbFailureClassifier`:** `case "HOR_002" -> GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE` (`ErrorKind.CONFLICT`, HTTP 409). Antes, `HOR_002` caía en `default` y respondía `ERR_DB_UNCLASSIFIED` (500).
- **`DbExceptionTranslatorTest`:** caso parametrizado `HOR_002, ERR_CRUCE_HORARIO_DOCENTE`.

No hay endpoints nuevos, ni cambios en `access-catalog.json` ni en el contrato del SP (firma y resultset iguales).

## Validación

`mvnw -B verify` (2026-10-29): 2682 pruebas, 0 fallos y 0 errores. JaCoCo cumple los umbrales. `BUILD SUCCESS`.
