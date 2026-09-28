---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Mapa del conocimiento del backend

Entrada operativa: [AGENTS](../AGENTS.md). **NORMATIVE** significa regla activa; AS-IS es evidencia del comportamiento actual; histórico no rige tareas nuevas. Precedencia en [SOURCE_OF_TRUTH](governance/SOURCE_OF_TRUTH.md).

## Governance

- **NORMATIVE:** [fuentes de verdad](governance/SOURCE_OF_TRUTH.md), [reglas de diseño API](governance/API_DESIGN_RULES.md), [Definition of Ready](governance/DEFINITION_OF_READY.md), [política documental](governance/DOCUMENTATION_POLICY.md), [higiene del repositorio](governance/REPOSITORY_HYGIENE.md).
- Contexto metodológico: [adaptación de Arquisoft](governance/REFERENCE_ADAPTATION.md).

## Baseline

- **NORMATIVE:** [LINEA_BASE](baseline/LINEA_BASE.md), [Golden Path](baseline/GOLDEN_PATH_ASISTENCIA.md), [Definition of Done única](baseline/DEFINITION_OF_DONE.md).
- Ledgers: [deuda técnica](baseline/TECHNICAL_DEBT.md), [validación manual](baseline/MANUAL_VALIDATION_LEDGER.md).

## Architecture

- **NORMATIVE:** [vista de paquetes](architecture/backend-package-structure.md), [estructura detallada](architecture/infrastructure-structure.md), [Composition Root](architecture/adapter-composition-standard.md), [servicios externos](architecture/external-services.md).
- **NORMATIVE:** [entrada/JSON estricto](architecture/input-validation.md), [respuestas de éxito](architecture/http-success-responses.md), [guía command/query](architecture/http-command-query-guidelines.md) (CF-001 explícito).

## Contracts

- **NORMATIVE:** [OpenAPI](contracts/OPENAPI_STANDARD.md), [eventos realtime](contracts/REALTIME_EVENT_STANDARD.md).
- AS-IS general: [matriz HTTP](contracts/HTTP_AS_IS_MATRIX.md). Contrato aprobado del Golden Path:
  [OpenAPI 3.1.2](contracts/openapi/openapi-golden-path.yaml) y
  [SHA-256](contracts/openapi/openapi-golden-path.sha256).

## Persistence

- **NORMATIVE, estrategia futura:** [JDBC → JPA incremental](persistence/JDBC_TO_JPA.md); no implementada.
- AS-IS: [inventario JDBC/test doubles](integration/repository-mock-inventory.md).

## Testing

- **NORMATIVE:** [TESTING_STANDARD](testing/TESTING_STANDARD.md).
- Runbooks: [VALIDATION_RUNBOOK](testing/VALIDATION_RUNBOOK.md), [guía de validación backend](testing/backend-validation-guide.md), [CI](../.github/CI.md).

## Security

- **NORMATIVE:** [runtime provider-neutral](security/runtime-security-provider-architecture.md), [provisioning de identidad](security/keycloak-identity-provider.md).
- Runbooks: [service account](security/keycloak-service-account.md), [infraestructura Keycloak LOCAL/DEV](../infra/keycloak/README.md).

## Observability

- **NORMATIVE:** [rutas de observabilidad](observability/README.md); enlaza arquitectura/config de logs, metrics, traces y correlation.

## Realtime

- **NORMATIVE:** [arquitectura Reactor/SSE](architecture/reactive-realtime.md), [contrato de eventos](contracts/REALTIME_EVENT_STANDARD.md).
- No se crea docs/realtime vacío: las fuentes actuales se mantienen en esas ubicaciones.

## Integration

- **NORMATIVE:** [protocolo de alineación contractual DB ↔ backend ↔ frontend](integration/CONTRACT_ALIGNMENT_PROTOCOL.md); plantilla [CONTRACT_MATRIX](../.claude/templates/CONTRACT_MATRIX.md).
- **NORMATIVE / AS-IS:** [Azure Runtime Integration](integration/azure-runtime-integration.md): Key Vault, App Configuration, Event Grid, caché local e invalidación.
- [SQL Server local](integration/sqlserver-connection.md), [alineación frontend pendiente de evidencia externa](integration/frontend-contract-alignment.md).

## ADR

- [ADR-001 Golden Path](adr/ADR-001-golden-path-asistencia.md).
- [ADR-002 incrementalidad JPA](adr/ADR-002-jpa-incremental.md).

## Work Items

- [Convención y artefactos](work-items/README.md), [LB-000](work-items/LB-000-gobernanza-documentacion/PLAN.md), [informe de consolidación](work-items/LB-000-gobernanza-documentacion/REPORT.md).
- [.claude/agents](../.claude/agents), [.claude/skills](../.claude/skills), [.claude/templates](../.claude/templates) son versionables; worktrees son locales.
- Skills: uco-arquitectura, uco-baseline, uco-contratos, [uco-catalogos](../.claude/skills/uco-catalogos/SKILL.md) (catálogos de mensajes/parámetros), [uco-azure](../.claude/skills/uco-azure/SKILL.md), uco-observabilidad, uco-persistencia, uco-realtime, uco-seguridad, uco-testing.

## Archive

- [Históricos y reemplazos](archive/README.md). Roadmaps, prompts y mapas de refactor sustituidos no autorizan implementación.

Azure Key Vault, App Configuration y Event Grid ya son capabilities implementadas y se documentan como AS-IS. Eso no prueba despliegue, IaC, CD ni validación cloud; esas evidencias y evoluciones siguen gobernadas por la línea base y MV-003.
