---
name: uco-contratos
description: Definir o revisar contratos HTTP, OpenAPI, schemas, errores, compatibilidad y eventos del backend.
---

# uco-contratos

## Cuándo usar

Definir o revisar contratos HTTP, OpenAPI, schemas, errores, compatibilidad y eventos del backend.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [OPENAPI_STANDARD.md](../../../docs/contracts/OPENAPI_STANDARD.md)
- [HTTP_AS_IS_MATRIX.md](../../../docs/contracts/HTTP_AS_IS_MATRIX.md)
- [REALTIME_EVENT_STANDARD.md](../../../docs/contracts/REALTIME_EVENT_STANDARD.md)
- [http-success-responses.md](../../../docs/architecture/http-success-responses.md)
- [input-validation.md](../../../docs/architecture/input-validation.md)
- [CONTRACT_ALIGNMENT_PROTOCOL.md](../../../docs/integration/CONTRACT_ALIGNMENT_PROTOCOL.md) y plantilla [CONTRACT_MATRIX](../../templates/CONTRACT_MATRIX.md) para comparar DB, backend y frontend

## Reglas obligatorias

Primero AS-IS, después diff, después decisión, después contrato TARGET; nunca un TARGET inventado al que luego se adapten DB o backend. Inventaría AS-IS y consumidores antes de TARGET. Solo OpenAPI aprobado es autoridad HTTP objetivo; el README no lo sustituye. No inventar estados, códigos, roles, formatos ni límites. Versionar breaking changes con decisión explícita. Una tarea `DOCUMENTATION_ONLY` o `CONTRACT_ANALYSIS` no escribe especificación funcional ni OpenAPI salvo que el PLAN lo autorice.

## Archivos y cambios prohibidos

El rol contratos no modifica src/main/**, src/test/**, SQL, pom.xml ni configuración productiva. No ajustar contrato para acomodar una implementación fallida.

## Quality gates

Compatibilidad y errores/seguridad/correlation/tiempo revisados; cuando exista spec, ejecutar su validación configurada y contract tests; no afirmar gate OpenAPI disponible hoy.

## Evidencia esperada

Matriz AS-IS/TARGET con fuentes, consumers, decisión/aprobación y bloqueos; cambios contractuales y resultados de validación.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.
