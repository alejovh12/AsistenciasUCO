---
name: uco-realtime
description: Revisar publicación de eventos y SSE local, contratos y límites de entrega o distribución.
---

# uco-realtime

## Cuándo usar

Revisar publicación de eventos y SSE local, contratos y límites de entrega o distribución.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [reactive-realtime.md](../../../docs/architecture/reactive-realtime.md)
- [adapter-composition-standard.md](../../../docs/architecture/adapter-composition-standard.md)
- [REALTIME_EVENT_STANDARD.md](../../../docs/contracts/REALTIME_EVENT_STANDARD.md)
- [runtime-security-provider-architecture.md](../../../docs/security/runtime-security-provider-architecture.md)

## Reglas obligatorias

Preservar RealtimePublisherPort neutral y adapter local Reactor/SSE actual. Polling periódico de frontend != realtime. Local/in-memory no resuelve múltiples instancias. No introducir WebSocket por preferencia ni broker sin ADR. Evento solo tras persistencia exitosa; cliente recupera estado por API. Bearer por header y scope por grupo.

## Archivos y cambios prohibidos

No Reactor/SSE/WebSocket/SDK broker en Domain/Application. No tokens en URL ni stream público; no modificar frontend, provider o contrato fuera del plan. No inventar garantías replay/durabilidad.

## Quality gates

Tests publisher, orden tras persistencia, ausencia de evento en error, autorización/filtrado por grupo, heartbeat/cancelación y métricas; verify/ArchUnit; E2E cuando aplica.

## Evidencia esperada

Schema/payload observado, garantías/límites, pruebas de scope/entrega y recuperación, compatibilidad del consumidor o bloqueo de evidencia.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.
