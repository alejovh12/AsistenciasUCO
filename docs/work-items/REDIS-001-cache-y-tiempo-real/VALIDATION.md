---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-25
---

# REDIS-001 - VALIDATION

Fecha: 2026-11-25. Rama `feature/redis-cache-tiempo-real` sobre `develop` a43639de. JDK 25 (Temurin 25.0.5).

| Verificación | Comando | Resultado |
| :--- | :--- | :--- |
| Suite completa + ArchUnit + JaCoCo | `LC_ALL=C.UTF-8 ./mvnw -B verify` | **BUILD SUCCESS**: 5295 pruebas, 0 fallos, 0 errores, 4 omitidas (integración con Redis real sin `REDIS_TEST_HOST`). JaCoCo LINE 95.95 %, BRANCH 87.31 % (gate 80/70) |
| Pruebas del work item | `./mvnw test -Dtest='CaffeineCacheConsultasTest,RedisCacheConsultasTest,SerializacionDtosCacheadosTest,ConsultasCacheadasPostProcessorTest,InvalidacionCachePorEventosTest,RedisLimitadorIntentosAdapterTest,CacheAdapterConfigurationTest,RedisRealtimeRelayTest,RedisSseRealtimeAdapterConfigurationTest'` | Verde (C-01..C-09, R-01..R-04, W-01) |
| Redis real (I-01) | `REDIS_TEST_HOST=127.0.0.1 REDIS_TEST_PORT=6390 ./mvnw test -Dtest=RedisRealIntegrationTest -Dgroups=integration` contra `redis-server` 7 local | 4/4: caché y TTL compartidos entre dos réplicas, invalidación por etiqueta desde otra réplica, límite del QR compartido, evento SSE entregado una vez en cada réplica |
| Matriz HU → pruebas | `python3 scripts/testing/matriz_hu_pruebas.py` | `MATRIZ_HU_PRUEBAS_PASS` (811 archivos) |
| Infra | `docker compose config` (raíz y `stack/asis-cache.compose.yml`) | Válidos |

Notas:

- `ContentSecurityValidatorTest.filename_con_espacios_y_caracteres_especiales_se_sanitiza` depende de la configuración regional:
  falla con `LANG` vacío y pasa con `C.UTF-8` (como en CI). No relacionado con este work item.
- Trivy de las dependencias nuevas (`spring-data-redis` 4.0.7, `lettuce-core` 6.8.2) lo valida el CI.
- Prueba manual en el laboratorio: ver la guía, §4 y el resumen para el owner (`infra.ps1 stack up cache`).

**Dictamen:** APTO para PR a `develop`. Bloqueos: ninguno. Pendiente de decisión del owner: canal SSE por usuario (TD-084).
