---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# Observabilidad

Capacidades obligatorias: **logs, metrics, traces y correlation ID**. La arquitectura extensa se mantiene en [Infrastructure §8](../architecture/infrastructure-structure.md#8-neutralidad-de-vendor-de-observabilidad), [Composition Root §12](../architecture/adapter-composition-standard.md#12-audit-y-observabilidad) y [realtime §7](../architecture/reactive-realtime.md#7-observabilidad).

Evidencia operacional configurada, no prueba de despliegue: [application.yml](../../src/main/resources/application.yml), [stack local](../../infra/observability/compose.yaml), [Alloy](../../infra/observability/alloy/config.alloy), [Prometheus](../../infra/observability/prometheus/prometheus.yml) y [datasources](../../infra/observability/grafana/provisioning/datasources/datasources.yml). Logs locales son JSONL leídos por Alloy; métricas se exponen para scrape y trazas salen por OTLP.

No romper `traceId`, `spanId` ni `correlationId` sin decisión arquitectónica explícita. No colocar SDKs en Application ni IDs de alta cardinalidad como labels. Auditoría de negocio conserva su contrato aparte. Ver [skill](../../.claude/skills/uco-observabilidad/SKILL.md), [runbook](../testing/VALIDATION_RUNBOOK.md) y [MV-003](../baseline/MANUAL_VALIDATION_LEDGER.md) para evidencia de ambiente.

Para Azure, la evidencia sanitizada identifica tipo de evento, capability/provider, clasificación de fallo, procesamiento Event Grid e invalidación de cache cuando aplique; nunca valores secretos. El logging AS-IS del webhook no implica métricas Azure ni operación cloud certificada. Detalle: [Azure Runtime Integration](../integration/azure-runtime-integration.md).

## Scrape de Prometheus (puerto de gestión)

Actuator y `/actuator/prometheus` salen por el puerto de gestión `${MANAGEMENT_SERVER_PORT:8090}`, no por el de la API (8080). La dirección de escucha es `management.server.address: ${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}`: por defecto solo loopback (valor seguro). [prometheus.yml](../../infra/observability/prometheus/prometheus.yml) raspa `host.docker.internal:8090`.

Seguridad del puerto de gestión (VAL H2, 2026-11-13): `ManagementEndpointsSecurityConfiguration` registra una cadena de Spring Security propia, evaluada antes que la de la API y aplicada **solo** a las peticiones que entran por el puerto de gestión real (`ManagementPortRequestMatcher`, usa `local.management.port` y nunca coincide si gestión y API comparten puerto). En ese puerto `GET /actuator/health`, `/actuator/health/**` y `/actuator/prometheus` responden sin credenciales (el scrape de Prometheus y las sondas de Traefik no llevan Bearer); cualquier otra ruta o método responde 403. En el puerto de la API nada cambia: `/actuator/prometheus` no existe ahí y sigue exigiendo Bearer (401). Antes de este cambio el scrape recibía 401 porque la cadena de la API también cubría el contexto de gestión. La protección de `/actuator/prometheus` es de red: el puerto de gestión escucha en loopback por defecto y no se publica fuera de la máquina o de la red interna. Prueba: `ManagementPortActuatorSecurityTest`.

Para el stack local de observabilidad (Prometheus corre en Docker y no alcanza el loopback del host) hay que arrancar el backend con `MANAGEMENT_SERVER_ADDRESS=0.0.0.0`. Hacerlo **solo en desarrollo** y con el puerto 8090 nunca expuesto fuera de la máquina (firewall o red local); en cualquier otro entorno se deja el valor por defecto.

Verificación en el laboratorio (VAL-oleada8, 2026-11-15): dentro del contenedor `asis-app-backend`,
`GET http://127.0.0.1:8090/actuator/prometheus` responde `200` sin credenciales y expone `http_server_requests_seconds_bucket`,
`hikaricp_*`, `jvm_*`, `notification_dispatch_total`, `notification_email_send_total`, `notification_email_throttled_total`,
`notification_publish_total` y `realtime_*`.

## Dashboard y alertas

- **Dashboard de métricas** (OBS-11): [asistencias-uco-metricas.json](../../infra/observability/grafana/provisioning/dashboards/asistencias-uco-metricas.json),
  carpeta "Asistencias UCO", 20 paneles. Salud (`up`), peticiones por estado, 5xx, latencia p50/p95/p99 y rutas más lentas,
  pool Hikari, heap e hilos de la JVM, despachos del outbox, envíos SMTP, clientes/eventos SSE y hechos de negocio. Variables `ds` (origen Prometheus) y `job`,
  para que sirva con el job `asistencias-uco` de este stack y con `asistencias-backend` del stack de Infra. El dashboard de logs
  (Loki) sigue en `asistencias-uco-dashboard.json`.
- **Alertas** (OBS-12): viven en `AsistenciasUCO-Infra/components/observability/prometheus/alert-rules.yml`, con pruebas de
  `promtool test rules` en el CI de Infra (backend caído, 5xx, latencia, pool, heap, outbox y SMTP).

## Métricas de negocio (OBS-07)

`asistencias_negocio_eventos_total{tipo}` cuenta cada hecho de negocio cuando se publica su evento de dominio, haya o no
clientes SSE conectados: `TOMA_ABIERTA`, `TOMA_CERRADA`, `ASISTENCIAS_TOMA_ACTUALIZADAS`, `ASISTENCIA_QR_REGISTRADA`,
`RECLAMO_ASISTENCIA_RADICADO`, `RECLAMO_ASISTENCIA_RESUELTO`, `SESION_CANCELADA`, `SESION_MANUAL_CREADA`,
`REVISION_CANCELACION_RESUELTA`, `ALERTA_RIESGO_INASISTENCIA` y `ALERTA_PERDIDA_INASISTENCIA`. La lista es cerrada
(`ReactorRealtimeAdapter.TIPOS_NEGOCIO`) para acotar la cardinalidad; los eventos técnicos (rotación del QR, catálogos) no
crean series. Vive en el proveedor `local-sse`, el único proveedor realtime; otro proveedor debe registrar la misma métrica.
Prueba: `MetricasNegocioRealtimeTest`.

## Salud de dependencias (OBS-10)

`/actuator/health` (puerto de gestión) muestra, además de `db` y `circuitBreakers`:

| Componente | Qué comprueba | En `readiness` |
| :--- | :--- | :--- |
| `smtp` (solo con `app.providers.outbox-notification.sender=smtp`) | Abre y cierra una conexión con el servidor SMTP (`testConnection`), sin enviar nada | No: si el SMTP cae, el outbox reintenta y la API sigue atendiendo |
| `emisorIdentidad` | `GET <issuer>/.well-known/openid-configuration` responde 200 en 3 s | No: los tokens se validan con las claves ya cargadas |

El grupo `readiness` sigue siendo `readinessState,db,circuitBreakers` (el PEP queda cubierto por su circuit breaker). Los
detalles solo llevan host, puerto, emisor, estado HTTP y el tipo de error; nunca el mensaje de la excepción. Pruebas:
`SmtpHealthIndicatorTest` y `EmisorIdentidadHealthIndicatorTest`.

## Muestreo de trazas (OBS-09)

`management.tracing.sampling.probability` se fija con `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` (por defecto `1.0`, todas las
trazas). Valores recomendados por ambiente:

| Ambiente | Valor | Motivo |
| :--- | :--- | :--- |
| Desarrollo local y laboratorio | `1.0` | Se necesita cada traza para diagnosticar. |
| Pruebas de carga y staging | `0.25` | Volumen representativo sin saturar Tempo. |
| Producción | `0.1` | Una de cada diez peticiones; los errores se siguen viendo en logs y métricas con su `correlationId`. |
