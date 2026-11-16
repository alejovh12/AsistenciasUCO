---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# VAL-oleada7-backend - VALIDATION

Fecha: 2026-11-15, revalidado el 2026-11-16. Trabajo original en `fix/VAL-oleada7-backend` sobre `repo-antiguo/develop`
aa553578; el 2026-11-16 se pasó a `fix/VAL-oleada7-cache-pep` sobre develop 8530bd54 (sin conflictos) y se retiró el
commit de la ruta literal (ver H). Windows 11, Java 25, `mvnw.cmd` con `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp`.

## C. H4: límite total de la consulta al PEP

| Prueba | Antes del cambio | Después |
| :--- | :--- | :--- |
| `pep_que_acepta_la_conexion_y_no_responde_se_corta_en_request_timeout` (socket que acepta y no responde, `request-timeout` 500 ms) | PASS (el cliente ya cortaba) | PASS, 0,53 s |
| `pep_que_envia_encabezados_y_deja_el_cuerpo_a_medias_se_corta_en_request_timeout` (encabezados incompletos; 200 con `Content-Length: 64` y 7 bytes) | **FAIL**: `assertTimeoutPreemptively` vencido a los 5 s, el cliente seguía esperando | PASS, 1,02 s (dos casos de 500 ms) |
| `SecurityBaselinePepClientTest` (9), `SecurityBaselinePepCircuitBreakerTest` (4), `SecurityPepEnforcementFilterTest` (6), `SecurityBaselinePepPropertiesTest` (10), `SecurityBaselinePepWiringTest` (13) | — | 42 de 42 PASS |

Conclusión: `HttpRequest.timeout` no cubría una respuesta a medias. Con `sendAsync(...).get(request-timeout)` y
`cancel(true)` al vencer, la decisión es UNAVAILABLE (503, fail-closed) dentro del límite configurado.

## D. Cachés SQL con vencimiento

| Prueba | Resultado |
| :--- | :--- |
| `CatalogoSqlCacheVencimientoTest` (5): parámetro vigente desde caché y vencido releído; invalidación explícita dentro del TTL; mensaje de usuario y técnico vencidos releídos; TTL 0, negativo o mayor que 24 h rechazado; reloj o TTL nulos rechazados | 5 de 5 PASS |
| `ParameterCatalogJpaBehaviorTest` (10), `MessageCatalogJpaBehaviorTest` (7), `MessageCatalogJpaMarcadoresSecuencialesTest` (4), `ParametroConValorPorDefectoCatalogAdapterTest` (7) | PASS sin cambios en las pruebas |
| Pruebas de arquitectura (`co.edu.uco.asistenciasuco.architecture`) | PASS |

## H. Inventario de rutas del PDP

| Prueba | Antes | Después |
| :--- | :--- | :--- |
| `python -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"` | 61 pruebas, 3 errores (`ExtractionError` en `MensajesInterfazPublicaController`) | 61 de 61 OK |
| `provision_asistencias.py catalog` | `ExtractionError` | 192 endpoints, 0 sin regla, 0 obsoletos |
| `MensajesInterfazPublicaControllerRutaTest` (literal = `RUTA_BASE`) | — | PASS |

**2026-11-16:** el PR #153 (`fix/extractor-constantes-ruta`, ya en develop) hizo que `extract_endpoints.py` resuelva
constantes de ruta, así que el commit de la ruta literal en `MensajesInterfazPublicaController` y su prueba se retiraron de
esta rama. Sobre develop 8530bd54, sin ese commit: suite `test_*.py` 66 de 66 OK y `provision_asistencias.py catalog` con
193 endpoints, 0 sin regla, 0 obsoletos.

## Gate completo

| Corrida | Resultado |
| :--- | :--- |
| `mvnw.cmd -o verify` (primera) | 4630 pruebas, 2 fallos en `JpaRepositoryBeanUniquenessTest`: el contexto mínimo de esa prueba no tiene el servicio de conversión de Spring Boot y `@Value` no convertía `"60s"` a `Duration` en los repositorios SQL. Se corrigió el código (texto + `DurationStyle`), no la prueba |
| `mvnw.cmd -o clean verify` (final) | **BUILD SUCCESS**: 4632 pruebas, 0 fallos, 0 errores; `All coverage checks have been met` (JaCoCo); 10:10 min |
| `mvnw.cmd -B -ntp clean verify` sobre develop 8530bd54, sin el commit de la ruta literal (2026-11-16) | **BUILD SUCCESS**: 4721 pruebas, 0 fallos, 0 errores, 0 omitidas; `All coverage checks have been met`; 11:30 min |

Las pruebas de integración (`-Pintegration`) no se corrieron: necesitan la BD del laboratorio, que está en uso para el
conjunto de datos de pruebas manuales; el cambio no toca SQL. Quedan para la CI del PR.

## Cambio de comportamiento a revisar

- Cliente del PEP: la llamada pasa de `send` a `sendAsync` con límite total. Con el PEP sano no cambia nada (mismas
  respuestas y encabezados); con el PEP lento, la decisión UNAVAILABLE llega a lo sumo en `request-timeout` (5 s por defecto).
- Catálogos SQL: con un único servidor, un valor cambiado directamente en la tabla (sin la API) ahora se toma en 60 s
  como máximo; antes nunca hasta reiniciar.
