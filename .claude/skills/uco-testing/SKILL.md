---
name: uco-testing
description: Derivar pruebas RED de requisitos y contratos, y validar tests, cobertura, arquitectura, integración y E2E.
---

# uco-testing

## Cuándo usar

Derivar pruebas RED de requisitos y contratos, y validar tests, cobertura, arquitectura, integración y E2E.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [TESTING_STANDARD.md](../../../docs/testing/TESTING_STANDARD.md)
- [VALIDATION_RUNBOOK.md](../../../docs/testing/VALIDATION_RUNBOOK.md)
- [backend-validation-guide.md](../../../docs/testing/backend-validation-guide.md)
- [DEFINITION_OF_DONE.md](../../../docs/baseline/DEFINITION_OF_DONE.md)

## Reglas obligatorias

REQUIREMENT → CONTRACT → TEST_PLAN → RED → IMPLEMENTATION → GREEN → VALIDATION. El tester puede inspeccionar fronteras/tests AS-IS, no diseñar assertions para confirmar una implementación terminada. Si un RED parece incorrecto: TEST_CONTRACT_CONFLICT; auditor dictamina y tester corrige con trazabilidad.

## Archivos y cambios prohibidos

Tester: no src/main/**, contratos aprobados, pom.xml ni runtime config. Implementador: no editar tests RED. Auditor: no editar código, tests ni contrato. Prohibido deshabilitar tests o rebajar gates.

## Quality gates

Gates reales en TESTING_STANDARD: verify, ArchUnit, líneas ≥80 %/ramas ≥70 %, integración cuando aplica y revisión de skips.

## Evidencia esperada

TEST_PLAN con escenarios negativos observables, falla RED causal, archivos/hash aprobados, GREEN, contadores JaCoCo y VALIDATION con exit code/limitaciones.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.
