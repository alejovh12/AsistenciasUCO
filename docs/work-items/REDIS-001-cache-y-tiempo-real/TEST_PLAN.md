---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-25
---

# REDIS-001 - TEST_PLAN

| ID | Conducta | Prueba |
| :--- | :--- | :--- |
| C-01 | Una clave repetida no vuelve a la fuente dentro del TTL; vence con el TTL; `null` no se guarda; invalidar recarga | `CaffeineCacheConsultasTest`, `RedisCacheConsultasTest` |
| C-02 | Redis caído/ilegible: la consulta se atiende desde la BD y se cuenta `error` | `RedisCacheConsultasTest` |
| C-03 | Muchas peticiones simultáneas a una clave ausente cargan una sola vez; si la carga falla, todas reciben la falla | `RedisCacheConsultasTest` |
| C-04 | Cada DTO cacheado vuelve idéntico de JSON (todos los campos, por reflexión) | `SerializacionDtosCacheadosTest` |
| C-05 | El portal se cachea por usuario y nunca mezcla usuarios; indicadores por filtro completo; catálogos por método | `ConsultasCacheadasPostProcessorTest` |
| C-06 | Registro por QR nuevo invalida materias y resumen; uno repetido no | `ConsultasCacheadasPostProcessorTest` |
| C-07 | Límite por minuto compartido; hash de la clave; respaldo local con Redis caído | `RedisLimitadorIntentosAdapterTest` |
| C-08 | Etiquetas: invalidar `grupo:<id>` borra solo sus claves (local y Redis), vence con el TTL, Redis caído cuenta `error` | `CaffeineCacheConsultasTest`, `RedisCacheConsultasTest` |
| C-09 | Eventos de grupo recargan el portal del estudiante (materias y resumen) y del docente; otro grupo, otros tipos o grupo inválido no; alerta invalida al destinatario; matrícula por QR invalida al estudiante; un observador roto no corta la entrega | `InvalidacionCachePorEventosTest` |
| R-01 | El relevo publica en el canal con la correlación de origen y no emite localmente | `RedisRealtimeRelayTest` |
| R-02 | La suscripción entrega a los clientes SSE de la réplica sin reemplazar la correlación | `RedisRealtimeRelayTest` |
| R-03 | Redis caído: entrega solo local, sin excepción al caso de uso; hecho de negocio contado una vez | `RedisRealtimeRelayTest` |
| R-04 | Mensaje ilegible o de otra versión: se descarta sin cortar la suscripción | `RedisRealtimeRelayTest` |
| U-01 | Canal por usuario: entrega al destinatario y no a otros; tipos sin audiencia `DESTINATARIO` no; solicitudes de sesión solo a titular/ámbito, una consulta por grupo; estudiante sin consulta; fallo del SP reintenta; mismo formato SSE | `CanalUsuarioRealtimeTest`, `RealtimeEventsControllerTest` |
| U-02 | `/realtime/stream/usuario` exige autenticación y la admite para cualquier rol; está en el catálogo del PEP | `RbacSecurityFilterChainTest`, `AccessCatalogConsistencyTest`, `test_catalog_tools.py` |
| W-01 | Selección de proveedor y defaults/validación de propiedades | `CacheAdapterConfigurationTest`, `RedisSseRealtimeAdapterConfigurationTest` |
| I-01 | Contra Redis real: dos réplicas comparten caché y TTL, invalidación por etiqueta, el límite y los eventos SSE (sin duplicados) | `RedisRealIntegrationTest` (requiere `REDIS_TEST_HOST`; fuera de CI) |
| A-01 | Reglas de arquitectura sin cambios | `co.edu.uco.asistenciasuco.architecture.*` |
