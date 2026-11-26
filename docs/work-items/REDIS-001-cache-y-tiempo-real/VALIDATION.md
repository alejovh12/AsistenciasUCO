---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-26
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

## Correcciones del code review (2026-11-26)

Rama `fix/code-review-redis-001` sobre `develop` f19e9f8f. Corrige los 10 hallazgos del `/code-review` de develop (informe
`CODE_REVIEW_BACKEND_DEVELOP_2026-11-26.md` del proyecto) sin cambiar contratos HTTP ni el comportamiento con un solo nodo:

| # | Hallazgo | Corrección | Pruebas |
| :--- | :--- | :--- | :--- |
| 1 | Relevo SSE podía duplicar o perder eventos en la réplica de origen | Entrega local directa + `origen` en el mensaje; el eco propio se descarta | `RedisRealtimeRelayTest` (eco propio, timeout tras aceptar), `RedisRealIntegrationTest` |
| 2 | Llave del limitador sin TTL si fallaba `EXPIRE` | Script Lua atómico con `PEXPIRE` en cada intento | `RedisLimitadorIntentosAdapterTest`, `RedisRealIntegrationTest` (TTL real) |
| 3 | Ventana fija: el doble de intentos en el cambio de minuto | Ventana deslizante de 60 s (sorted set), solo cuentan los aceptados | `RedisRealIntegrationTest.la_ventana_es_deslizante_...` |
| 4 | Acierto de caché saltaba la validación de ámbito | Huella del ámbito vigente en la clave de indicadores | `ConsultasCacheadasPostProcessorTest` (cambio de ámbito, sin puerto, falla) |
| 5 | Con Redis caído se esperaba el timeout en cada petición | `DisyuntorRedis` (30 s) en caché y limitador; invalidaciones pendientes se aplican al volver | `DisyuntorRedisTest`, `RedisCacheConsultasTest`, `RedisLimitadorIntentosAdapterTest` |
| 6 | Invalidación durante una carga se perdía | Contador de invalidaciones: la carga que lo ve cambiar borra lo que guardó (Redis y Caffeine) | `RedisCacheConsultasTest`, `CaffeineCacheConsultasTest` |
| 7 | Resumen y plan quedaban sin etiqueta de grupo | Se etiquetan con los grupos de las materias del estudiante | `ConsultasCacheadasPostProcessorTest` |
| 8 | Caché `local` por defecto con varias réplicas | Se mantiene el predeterminado (decisión REDIS-001) y se advierte al arrancar con `local` + `local-sse`; guía §1 | `CacheAdapterConfigurationTest` |
| 9 | Ámbito del canal del usuario recordado toda la conexión | Vigencia de 60 s y máximo 256 grupos por conexión | `CanalUsuarioRealtimeTest` |
| 10 | Retiro de estudiante y reclamo resuelto no invalidaban | Decoradores de `RetirarEstudianteGrupo` y `ResolverSolicitudRevisionAsistencia` | `ConsultasCacheadasPostProcessorTest` |

No se cambió (queda para decisión): la observación del hallazgo 5 sobre mover la invalidación fuera del hilo que publica, porque
invalidar antes de avisar al cliente es lo que evita que el cliente recargue y reciba el dato viejo; con el disyuntor la espera
queda acotada a la primera falla. Del hallazgo 9, resolver la audiencia una vez por evento para todos los suscriptores no aplica:
la audiencia es por usuario.

Notas del security review del mismo día (confianza baja, sin cambio de código):

- `InstitutionalScopeJpaRepository.SQL_ESTUDIANTE_ACCEDE_GRUPO` cuenta matrículas en cualquier estado (`uv_estudiante_grupo` no
  filtra estado). Solo lo usa `ConsultarSesion` (detalle de una sesión); el canal SSE usa `esEstudianteActivo`. Filtrar por
  estado `A` dejaría sin historial a estudiantes `F` (finalizado): es decisión funcional, no se cambió.
- `usp_cambiar_estado_programa_academico` ya valida que el programa sea de la facultad del decano ejecutor antes de mutar
  (BD develop 7efc83d), así que el orden en `CambiarEstadoProgramaUseCaseImpl` no permite cambiar programas de otra facultad.

Validación de esta rama: ver la tabla siguiente.

| Verificación | Comando | Resultado |
| :--- | :--- | :--- |
| Suite completa + ArchUnit + JaCoCo | `LC_ALL=C.UTF-8 mvn -B verify` (JDK 25, OpenJDK 25.0.5) | **BUILD SUCCESS**: 5330 pruebas, 0 fallos, 0 errores, 5 omitidas (Redis real sin `REDIS_TEST_HOST`). JaCoCo LINE 95.84 %, BRANCH 87.04 % (gate 80/70) |
| Redis real (I-01) | `REDIS_TEST_HOST=localhost mvn test -Dtest=RedisRealIntegrationTest -Dgroups=integration` contra `redis-server` local | 5/5, incluida la ventana deslizante y el TTL del limitador con el script Lua real |
| Matriz HU → pruebas | `python3 scripts/testing/matriz_hu_pruebas.py` | `MATRIZ_HU_PRUEBAS_PASS` |

Sonar, CodeQL y Trivy los valida el CI al abrir el PR.

### Ajuste por el Quality Gate de Sonar del PR #191 (2026-11-26)

Sonar marcó 79.5 % de cobertura en código nuevo y Reliability B. Se corrigió: campo `volatile` del decorador por
`AtomicReference` (bug de confiabilidad), método `portal` sin uso eliminado, complejidad cognitiva de
`postProcessAfterInitialization` reducida (tres métodos por tipo de consulta y extractores de actor/programa), literal repetido
en `RedisCacheConsultas`, nombre de campo en `DisyuntorRedis`, `Thread.sleep` e imports en pruebas. Pruebas nuevas: desborde de
invalidaciones pendientes (`SCAN` + `DEL`), valor ilegible sin poder borrarse, origen del codec y tope de ámbitos por conexión.
`LC_ALL=C.UTF-8 mvn -B verify`: BUILD SUCCESS, 5334 pruebas, 0 fallos; cobertura de líneas nuevas medida con JaCoCo: 91.3 %.
