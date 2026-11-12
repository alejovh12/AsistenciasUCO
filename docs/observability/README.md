---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# Observabilidad

Capacidades obligatorias: **logs, metrics, traces y correlation ID**. La arquitectura extensa se mantiene en [Infrastructure §8](../architecture/infrastructure-structure.md#8-neutralidad-de-vendor-de-observabilidad), [Composition Root §12](../architecture/adapter-composition-standard.md#12-audit-y-observabilidad) y [realtime §7](../architecture/reactive-realtime.md#7-observabilidad).

Evidencia operacional configurada, no prueba de despliegue: [application.yml](../../src/main/resources/application.yml), [stack local](../../infra/observability/compose.yaml), [Alloy](../../infra/observability/alloy/config.alloy), [Prometheus](../../infra/observability/prometheus/prometheus.yml) y [datasources](../../infra/observability/grafana/provisioning/datasources/datasources.yml). Logs locales son JSONL leídos por Alloy; métricas se exponen para scrape y trazas salen por OTLP.

No romper `traceId`, `spanId` ni `correlationId` sin decisión arquitectónica explícita. No colocar SDKs en Application ni IDs de alta cardinalidad como labels. Auditoría de negocio conserva su contrato aparte. Ver [skill](../../.claude/skills/uco-observabilidad/SKILL.md), [runbook](../testing/VALIDATION_RUNBOOK.md) y [MV-003](../baseline/MANUAL_VALIDATION_LEDGER.md) para evidencia de ambiente.

Para Azure, la evidencia sanitizada identifica tipo de evento, capability/provider, clasificación de fallo, procesamiento Event Grid e invalidación de cache cuando aplique; nunca valores secretos. El logging AS-IS del webhook no implica métricas Azure ni operación cloud certificada. Detalle: [Azure Runtime Integration](../integration/azure-runtime-integration.md).

## Scrape de Prometheus (puerto de gestión)

Actuator y `/actuator/prometheus` salen por el puerto de gestión `${MANAGEMENT_SERVER_PORT:8090}`, no por el de la API (8080). La dirección de escucha es `management.server.address: ${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}`: por defecto solo loopback (valor seguro). [prometheus.yml](../../infra/observability/prometheus/prometheus.yml) raspa `host.docker.internal:8090`.

Para el stack local de observabilidad (Prometheus corre en Docker y no alcanza el loopback del host) hay que arrancar el backend con `MANAGEMENT_SERVER_ADDRESS=0.0.0.0`. Hacerlo **solo en desarrollo** y con el puerto 8090 nunca expuesto fuera de la máquina (firewall o red local); en cualquier otro entorno se deja el valor por defecto.
