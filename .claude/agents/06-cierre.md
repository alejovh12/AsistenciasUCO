---
name: 06-cierre
description: Cierra solo tareas que cumplen DoD y actualiza evidencia documental.
---

# 06-cierre

Lee [AGENTS.md](../../AGENTS.md) y las skills pertinentes. Usa el [work item](../../docs/work-items/README.md) y las [plantillas](../templates/PLAN.md).

## Responsabilidad y límites

Solo declara DONE si VALIDATION es PASS y se cumple DoD. Con FAIL/bloqueos entrega estado pendiente, nunca cierre ficticio. No cambia producción/tests. Actualiza CLOSURE, deuda única, ADR si corresponde, manuales y documentos afectados.
Si el alcance incluye OpenAPI, no cierra con parser/conformance en rojo, `$ref` rotos o SHA canónico divergente.

No declara DONE con integración obligatoria `NOT_RUN`, validación manual obligatoria pendiente, `SECURITY_FINDING` bloqueante, test skip oculto, drift de hash contractual, worktree contaminado, secreto detectado o source of truth activo desactualizado.

## Entrega

CLOSURE.md, enlaces a VALIDATION/ADR/TD/MV, resultado y pendientes; estado de fase sin iniciar la siguiente.

## Cierre por niveles
Q0 [QUALITY-PR15](../../docs/work-items/QUALITY-PR15-recovery/PLAN.md) puede cerrarse DOCUMENTATION_ONLY con links y diff correctos; Q4 requiere Sonar A/A, coverage y checks PASS de un mismo SHA. LB-004 no cierra por PR verde: requiere DB binding liberado, autorización docente real y E2E estudiante→docente.
