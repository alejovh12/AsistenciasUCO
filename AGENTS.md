---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# AsistenciasUCO — entrada para agentes

Sigue esta ruta: **AGENTS.md → skill correspondiente → documento normativo → work item → código/tests → validación**.
El [mapa humano](docs/README.md) y la [precedencia por ámbito](docs/governance/SOURCE_OF_TRUTH.md) identifican la autoridad.

## Principios obligatorios

- Preserva Clean Architecture y los puertos neutrales; Domain/Application no dependen de infraestructura ni frameworks. La estructura real está en [arquitectura](docs/architecture/backend-package-structure.md).
- Reutiliza evidencia existente. No inventes endpoints, clases existentes, SQL, roles, reglas de negocio, consumidores ni resultados de pruebas. Distingue AS-IS, TARGET y propuesta.
- El backend no administra el esquema DB. No cambies contratos públicos ni retires compatibilidad dentro de un refactor técnico sin decisión contractual explícita.
- No incorpores secretos a código, documentación, logs ni evidencias. Reporta `SECURITY_FINDING` con archivo/tipo, nunca el valor.
- Los tests prueban comportamiento observable, no detalles de implementación. Un mock no certifica un provider externo y una integración obligatoria `NOT_RUN` no es `PASS`.
- SDKs cloud solo viven en Infrastructure. Los valores reales de secretos/configuración externa nunca se convierten en source of truth del repositorio.
- Antes de una fase técnica mayor, las fuentes activas deben describir el AS-IS comprobado; documentación desactualizada bloquea el cierre, no autoriza adaptar el código a ella.
- `.claude/worktrees/` y `.workspace/` son estado local, sin autoridad. El [archivo histórico](docs/archive/README.md) tampoco rige tareas nuevas.

## Flujo y evidencia

Antes de crear o modificar una API, **MUST** leer
[API_DESIGN_RULES](docs/governance/API_DESIGN_RULES.md), el
[BACKEND_GOLDEN_PATH_CONTRACT](docs/contracts/BACKEND_GOLDEN_PATH_CONTRACT.md) cuando aplique y el
[OpenAPI canónico](docs/contracts/openapi/openapi-golden-path.yaml). No inventar endpoints ni
usar annotations Java como fuente primaria del contrato.

1. Abre o crea el [work item](docs/work-items/README.md) de la fase en [LINEA_BASE](docs/baseline/LINEA_BASE.md). Usa las [plantillas](.claude/templates/PLAN.md).
2. Planifica clase de cambio, rutas permitidas/prohibidas, variable principal, alcance/no alcance, evidencia, consumidores, riesgos y rollback. Comprueba la [Definition of Ready](docs/governance/DEFINITION_OF_READY.md): `NOT_READY` impide implementar.
3. Congela el contrato aplicable; sigue **REQUIREMENT → CONTRACT → TEST_PLAN → RED → GREEN → VALIDATE** conforme al [estándar de testing](docs/testing/TESTING_STANDARD.md).
4. El tester deriva pruebas del requisito/contrato. **El implementador no modifica los tests RED para hacer pasar su solución.** Si un test contradice el contrato: `TEST_CONTRACT_CONFLICT`, detener el cambio afectado y devolverlo a contratos/tester tras dictamen del auditor.
5. Valida con el [runbook](docs/testing/VALIDATION_RUNBOOK.md) y registra comandos, fecha, salida/exit code, cobertura, alcance y limitaciones en `VALIDATION.md`. Una prueba no ejecutada no es PASS.
6. Cierra solo al cumplir la [DoD única](docs/baseline/DEFINITION_OF_DONE.md), con `CLOSURE.md`, [deuda](docs/baseline/TECHNICAL_DEBT.md), [manuales](docs/baseline/MANUAL_VALIDATION_LEDGER.md) y ADR cuando corresponda.

Responsabilidades: [planificador](.claude/agents/01-planificador.md), [contratos](.claude/agents/02-contratos.md), [tester RED](.claude/agents/03-tester-red.md), [implementador](.claude/agents/04-implementador.md), [auditor](.claude/agents/05-auditor.md), [cierre](.claude/agents/06-cierre.md). La revisión es conceptualmente independiente y no corrige durante la auditoría.

## Cuándo detenerse

- `BLOCKED_BY_MISSING_EVIDENCE`: falta una fuente necesaria; registra qué falta, dónde se buscó, qué bloquea y quién debe aportarla.
- `CONTRACT_CONFLICT`: dos fuentes autoritativas se contradicen; registra ambas y su ámbito. No elijas ni cambies código para resolverlas por suposición.
- Continúa únicamente trabajo independiente del bloqueo. Su resolución y aprobación deben quedar versionadas en el work item.

## Skills disponibles

| Tarea | Skill |
|---|---|
| Capas, puertos, wiring y providers | [uco-arquitectura](.claude/skills/uco-arquitectura/SKILL.md) |
| Fases, Golden Path, deuda y cierre | [uco-baseline](.claude/skills/uco-baseline/SKILL.md) |
| HTTP, schemas, errores y eventos | [uco-contratos](.claude/skills/uco-contratos/SKILL.md) |
| JDBC y estrategia incremental JPA | [uco-persistencia](.claude/skills/uco-persistencia/SKILL.md) |
| RED, cobertura, ArchUnit, integración y E2E | [uco-testing](.claude/skills/uco-testing/SKILL.md) |
| JWT, roles, ownership y secretos | [uco-seguridad](.claude/skills/uco-seguridad/SKILL.md) |
| Logs, metrics, traces y correlation ID | [uco-observabilidad](.claude/skills/uco-observabilidad/SKILL.md) |
| Puerto realtime, SSE y entrega de eventos | [uco-realtime](.claude/skills/uco-realtime/SKILL.md) |
| Catálogos de mensajes y parámetros | [uco-catalogos](.claude/skills/uco-catalogos/SKILL.md) |
| Azure Key Vault, App Configuration y Event Grid | [uco-azure](.claude/skills/uco-azure/SKILL.md) |

Apoyo: [protocolo de alineación contractual](docs/integration/CONTRACT_ALIGNMENT_PROTOCOL.md) y [plantilla CONTRACT_MATRIX](.claude/templates/CONTRACT_MATRIX.md) para comparar contratos entre repositorios; [higiene del repositorio](docs/governance/REPOSITORY_HYGIENE.md).

## Validación mínima

Java 25. Windows: `.\mvnw.cmd verify`; Unix: `./mvnw verify` (o `mvn verify` con Maven instalado).
Incluye tests, ArchUnit y gates JaCoCo **LINE ≥80 % / BRANCH ≥70 %** del `pom.xml`.
Si cambia persistencia/integración: `./mvnw -Pintegration verify`, con DB controlada y fixtures; revisar skips.
Los checks CI remotos y la validación manual aplicables se documentan; no se presumen ejecutados localmente.

El work item activo determina el alcance autorizado.
Ninguna fase comienza automáticamente.
LINEA_BASE + Definition of Ready + work item activo determinan qué puede modificarse.
`NOT_READY` impide implementación.
