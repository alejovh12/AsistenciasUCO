---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Definition of Ready

Todo `PLAN.md` declara clase de cambio (`DOCUMENTATION_ONLY`, `CONTRACT_ANALYSIS`, `CONTRACT_CHANGE`, `REFACTOR`, `BEHAVIOR_CHANGE`, `PERSISTENCE_MIGRATION`, `INFRASTRUCTURE`), rutas permitidas, rutas prohibidas y variable principal. Nada fuera de las rutas permitidas se modifica.

## READY para `CONTRACT_ANALYSIS`

No exige build verde. Exige:

- repositorios/snapshots identificados y versiones/hashes registrados según el [protocolo de alineación](../integration/CONTRACT_ALIGNMENT_PROTOCOL.md);
- owner y consumer identificados;
- Golden Path definido y scope de objetos DB definido;
- ningún secreto en la evidencia;
- capacidad para inspeccionar provider y consumer.

## READY para implementación

Exige, además de lo siguiente: build base verde o excepción aprobada, conflictos contractuales resueltos, contrato aprobado y TEST_PLAN. Antes de escribir implementación, `PLAN.md` debe resolver:

- objetivo, criterios de aceptación, alcance y no alcance;
- fuentes autoritativas, evidencia AS-IS y TARGET separados;
- archivos existentes afectados y archivos propuestos marcados NUEVOS;
- contratos HTTP, dominio, persistencia, seguridad y eventos afectados;
- consumidores afectados y evidencia de compatibilidad (frontend cuando corresponda);
- riesgos, dependencias y ambiente/fixtures necesarios;
- estrategia de pruebas y TEST_PLAN trazable al requisito/contrato;
- rollback practicable y stop conditions;
- deuda conocida y validaciones manuales aplicables;
- cero `CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT` o `BLOCKED_BY_MISSING_EVIDENCE` abiertos relevantes para el alcance a implementar.

Resultado obligatorio: **READY** o **NOT_READY**, con justificación y referencias. `NOT_READY` permite investigar y planificar, pero prohíbe al implementador escribir código. Una tarea documental puede declarar contratos/tests funcionales NO APLICA explicando por qué, sin inventar un RED Java.

La aprobación de un contrato y del plan se identifica con decisión, responsable y referencia versionada; no se infiere de un archivo generado. Una orden de planificar o analizar una fase autoriza investigación, no elimina sus bloqueos ni autoriza la fase siguiente.

Un `TECHNICAL_BUILD_GATE` rojo (p. ej. build base rojo por [TD-029](../baseline/TECHNICAL_DEBT.md#td-029)) no impide continuar `DOCUMENTATION_ONLY` ni `CONTRACT_ANALYSIS`. Mientras esté rojo no puede iniciar implementación, salvo excepción explícita aprobada y versionada en el work item, ninguna de estas clases: `CONTRACT_CHANGE`, `REFACTOR`, `BEHAVIOR_CHANGE`, `PERSISTENCE_MIGRATION` (JPA está incluido) e `INFRASTRUCTURE`.
