---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-25
---

# REDIS-001 — Caché de consultas y tiempo real entre réplicas - PLAN

Fecha: 2026-11-25. Rama `feature/redis-cache-tiempo-real` desde `develop` a43639de (PR #189 incluido).
Infra: rama homónima en `AsistenciasUCO/AsistenciasUCO-Infra` (sección `asis-cache`, perfil `cache`). Frontend y BD: sin cambios.

**Autorización:** pedido de John (owner) el 2026-11-24: revisar qué casos necesitan tiempo real (WebSocket/SSE) y dónde aplicar
caché Redis y con qué estrategia; aprobó las 11 recomendaciones ("ejecuta todo") el 2026-11-25. Decisión en
[ADR-007](../../adr/ADR-007-redis-cache-y-relevo-realtime.md); análisis por módulo en
[cache-y-tiempo-real.md](../../architecture/cache-y-tiempo-real.md).

## Clase de cambio y variable principal

- Clase: técnica (infraestructura opcional), sin cambio de contrato HTTP, SQL ni Application.
- Variable principal: con dos réplicas, un evento SSE publicado en una llega a los clientes de la otra; una consulta repetida no
  vuelve a la BD dentro de su TTL.

## Alcance

| Ítem | Cambio |
| :--- | :--- |
| Caché | `CacheConsultas` (cache-aside) con proveedores `local` (Caffeine, defecto), `redis` y `none`; regiones `portal` 60 s, `indicadores` 5 min, `catalogos` 30 min |
| Decoradores | `ConsultasCacheadasPostProcessor`: 6 consultas del portal (estudiante/docente), 8 de indicadores, 4 catálogos; el registro por QR invalida materias y resumen del estudiante; la matrícula por QR, todo su portal |
| Invalidación por grupo | Etiquetas `grupo:<id>` en `CacheConsultas` (`etiquetar`/`invalidarEtiqueta`; SET en Redis, índice en Caffeine); `InvalidacionCachePorEventos` observa los eventos que entrega la réplica (`ReactorRealtimeAdapter.alEntregar`) y borra el portal del grupo (TD-083) |
| Límite del QR | `RedisLimitadorIntentosAdapter` (ventana de 1 min compartida) con respaldo en el limitador local |
| Realtime | Provider `redis-sse`: `RedisRealtimeRelayAdapter` + `RedisRealtimeSuscriptor` + `RedisRealtimeCodec`; `ReactorRealtimeAdapter` gana `prepararParaRelevo` y `entregarRelevado`; `LocalSseRealtimeStreamGateway.conRelevo` |
| Config | `spring.data.redis.*`, `app.adapters.cache.*`, `app.adapters.realtime.redis.canal`, `management.health.redis.enabled=false` por defecto; la suite normal usa `cache.provider=none` |

## Fuera de alcance

- Canal SSE por usuario (contadores globales sin estar en un grupo): TD-084 (cruza con notificaciones de Sergio, el PEP y el frontend; decisión de John pendiente).
- Cambios sin evento de grupo (p. ej. resolver una solicitud de revisión sin cruzar umbral): acotados por el TTL de 60 s (residual de TD-083).
- Caché de periodos, festivos, padrón y del PEP: candidatos en la guía (§3.3).
- Frontend: el contrato SSE no cambia; no hay cambios.

## Rutas

Permitidas: `infrastructure/cache/**`, `infrastructure/adapter/secondary/realtime/redis/**`,
`infrastructure/adapter/secondary/security/RedisLimitadorIntentosAdapter`, `infrastructure/config/adapters/{cache,realtime/redis}/**`,
`ReactorRealtimeAdapter`, `LocalSseRealtimeStreamGateway`, `CacheAdapterProperties`, `RealtimeAdapterProperties`,
`AdapterPropertiesConfiguration`, `application.yml` (main y test), `pom.xml`, documentación.
Prohibidas: Domain, Application, controllers, OpenAPI, SQL.

## Riesgos y rollback

| Riesgo | Mitigación |
| :--- | :--- |
| Dato viejo en pantalla | TTL corto en el portal, invalidación por QR, `APP_ADAPTERS_CACHE_PROVIDER=none` lo apaga |
| Redis caído | Degradación a BD / limitador local / entrega local; métricas `resultado=error` y `realtime.relay.fallback` |
| Fuga entre usuarios | Clave con usuario o filtro completo con actor; prueba `ConsultasCacheadasPostProcessorTest` |
| Valor de otra versión del DTO en Redis | Se descarta al no poder leerlo; prefijo `v1` |

Rollback: `APP_ADAPTERS_CACHE_PROVIDER=none` y `APP_ADAPTERS_REALTIME_PROVIDER=local-sse` (sin redeploy de código), o revertir el PR.

## DoR

READY: fuentes (TD-003, LB-004 DECISIONS, reactive-realtime, adapter-composition-standard) leídas; decisión del owner registrada;
sin contrato público afectado.
