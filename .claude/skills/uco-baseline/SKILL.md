---
name: uco-baseline
description: Planificar fases LB, Golden Path, deuda, validaciones manuales y cierre documental.
---

# uco-baseline

## Cuándo usar

Planificar fases LB, Golden Path, deuda, validaciones manuales y cierre documental.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [LINEA_BASE.md](../../../docs/baseline/LINEA_BASE.md)
- [GOLDEN_PATH_ASISTENCIA.md](../../../docs/baseline/GOLDEN_PATH_ASISTENCIA.md)
- [DEFINITION_OF_DONE.md](../../../docs/baseline/DEFINITION_OF_DONE.md)
- [TECHNICAL_DEBT.md](../../../docs/baseline/TECHNICAL_DEBT.md)
- [MANUAL_VALIDATION_LEDGER.md](../../../docs/baseline/MANUAL_VALIDATION_LEDGER.md)

## Reglas obligatorias

Conserva una variable principal por tarea. No convertir roadmap en changelog; detalles en work item. No iniciar fases posteriores por leer su estrategia. Cierra solo con DoD y evidencia.

## Archivos y cambios prohibidos

En tareas de gobernanza no tocar src/main/**, src/test/**, pom.xml, SQL, frontend, configuración runtime ni workflows. .gitignore solo si el alcance documental lo autoriza.

## Quality gates

DoR para implementar, DoD para cerrar, enlaces válidos y autoridad única; verify según runbook.

## Evidencia esperada

PLAN, TEST_PLAN, VALIDATION, CLOSURE, IDs de deuda/manuales y ADR cuando cambie una decisión duradera.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.
