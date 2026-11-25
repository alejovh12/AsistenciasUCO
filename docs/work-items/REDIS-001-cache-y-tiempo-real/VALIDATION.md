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
| Suite completa + ArchUnit + JaCoCo | `LC_ALL=C.UTF-8 ./mvnw -B verify` | **BUILD SUCCESS**: 5308 pruebas, 0 fallos, 0 errores, 4 omitidas (integración con Redis real sin `REDIS_TEST_HOST`). JaCoCo LINE 95.96 %, BRANCH 87.34 % (gate 80/70) |
| Pruebas del work item | `./mvnw test -Dtest='CaffeineCacheConsultasTest,RedisCacheConsultasTest,SerializacionDtosCacheadosTest,ConsultasCacheadasPostProcessorTest,InvalidacionCachePorEventosTest,CanalUsuarioRealtimeTest,RealtimeEventsControllerTest,RbacSecurityFilterChainTest,AccessCatalogConsistencyTest,RedisLimitadorIntentosAdapterTest,CacheAdapterConfigurationTest,RedisRealtimeRelayTest,RedisSseRealtimeAdapterConfigurationTest'` | Verde (C-01..C-09, R-01..R-04, U-01, U-02, W-01) |
| Redis real (I-01) | `REDIS_TEST_HOST=127.0.0.1 REDIS_TEST_PORT=6390 ./mvnw test -Dtest=RedisRealIntegrationTest -Dgroups=integration` contra `redis-server` 7 local | 4/4: caché y TTL compartidos entre dos réplicas, invalidación por etiqueta desde otra réplica, límite del QR compartido, evento SSE entregado una vez en cada réplica |
| Matriz HU → pruebas | `python3 scripts/testing/matriz_hu_pruebas.py` | `MATRIZ_HU_PRUEBAS_PASS` (812 archivos) |
| Catálogo del PEP | `python3 -I infra/security-baseline/scripts/test_catalog_tools.py` | 19 OK |
| Frontend (rama `feat/canal-usuario-realtime`) | `npm run test:ci`, `npm run lint`, `npm run lint:tokens` | 3914 pruebas en verde (Statements 95.43 %, Branches 84.76 %, Lines 96.29 %); lint y tokens OK |
| Infra | `docker compose config` (raíz y `stack/asis-cache.compose.yml`) | Válidos |

Notas:

- `ContentSecurityValidatorTest.filename_con_espacios_y_caracteres_especiales_se_sanitiza` depende de la configuración regional:
  falla con `LANG` vacío y pasa con `C.UTF-8` (como en CI). No relacionado con este work item.
- Trivy de las dependencias nuevas (`spring-data-redis` 4.0.7, `lettuce-core` 6.8.2) lo valida el CI.
- Prueba manual en el laboratorio: ver la guía, §4 y el resumen para el owner (`infra.ps1 stack up cache`).

**Dictamen:** APTO para PR a `develop` (backend, Infra y frontend). Bloqueos: ninguno. Antes de probar el canal por usuario en el
laboratorio hay que reprovisionar el PDP local (`provision_asistencias.py`, sin `--prune`) para registrar la ruta nueva.
