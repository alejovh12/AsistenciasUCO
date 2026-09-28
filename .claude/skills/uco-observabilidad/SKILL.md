---
name: uco-observabilidad
description: Preservar logs, metrics, traces y correlation ID al cambiar o revisar comportamiento del backend.
---

# uco-observabilidad

## Cuándo usar

Preservar logs, metrics, traces y correlation ID al cambiar o revisar comportamiento del backend.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [infrastructure-structure.md](../../../docs/architecture/infrastructure-structure.md)
- [adapter-composition-standard.md](../../../docs/architecture/adapter-composition-standard.md)
- [reactive-realtime.md](../../../docs/architecture/reactive-realtime.md)
- [README.md](../../../docs/observability/README.md)

## Reglas obligatorias

Logs, metrics, traces y correlation ID son capacidades obligatorias. No romper traceId/spanId/correlationId sin decisión arquitectónica explícita. Telemetría por OTel/Micrometer/logging, audit separado; no puertos por Grafana/Loki/Tempo/Prometheus. Application no importa SDKs. Sin IDs de alta cardinalidad como labels.

Para Azure registrar, cuando aplique: tipo de evento, correlation disponible, clasificación de fallo del provider, resultado de procesamiento Event Grid e invalidación de cache. Nunca valor secreto. Distinguir AS-IS observado de métricas/tracing TARGET; no inventar métricas que no existen.

## Archivos y cambios prohibidos

No secretos/PII en logs/eventos; no SDK de telemetría en Domain/Application. No cambiar application.yml, infra/observability/** o workflows fuera del PLAN. No sustituir correlation vigente por uno inventado.

## Quality gates

Tests de filtro/contexto/limpieza, propagación realtime y métricas; verify/ArchUnit. Smoke de ambiente solo cuando aplica y con evidencia.

## Evidencia esperada

Recorrido correlationId/traceId/spanId, nombres/semántica de métricas y prueba de logs/traces sanitizada; MV-003 si la operación externa no se valida.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.
