---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Observabilidad

Capacidades obligatorias: **logs, metrics, traces y correlation ID**. La arquitectura extensa se mantiene en [Infrastructure §8](../architecture/infrastructure-structure.md#8-neutralidad-de-vendor-de-observabilidad), [Composition Root §12](../architecture/adapter-composition-standard.md#12-audit-y-observabilidad) y [realtime §7](../architecture/reactive-realtime.md#7-observabilidad).

Evidencia operacional configurada, no prueba de despliegue: [application.yml](../../src/main/resources/application.yml), [stack local](../../infra/observability/compose.yaml), [Alloy](../../infra/observability/alloy/config.alloy), [Prometheus](../../infra/observability/prometheus/prometheus.yml) y [datasources](../../infra/observability/grafana/provisioning/datasources/datasources.yml). Logs locales son JSONL leídos por Alloy; métricas se exponen para scrape y trazas salen por OTLP.

No romper `traceId`, `spanId` ni `correlationId` sin decisión arquitectónica explícita. No colocar SDKs en Application ni IDs de alta cardinalidad como labels. Auditoría de negocio conserva su contrato aparte. Ver [skill](../../.claude/skills/uco-observabilidad/SKILL.md), [runbook](../testing/VALIDATION_RUNBOOK.md) y [MV-003](../baseline/MANUAL_VALIDATION_LEDGER.md) para evidencia de ambiente.
