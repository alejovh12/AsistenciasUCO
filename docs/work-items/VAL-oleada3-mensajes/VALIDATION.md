---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VALIDATION — VAL-oleada3-mensajes (backend)

Fecha: 2026-11-13. JDK 25.0.5, Maven offline.

| Prueba | Resultado |
| :--- | :--- |
| `test-compile` tras el reemplazo automático (189 sitios + 3 constantes + 12 llamadas a auxiliares) | OK |
| Pruebas afectadas: `MensajeUsuarioErrorCodeCatalogoTest`, `GlobalExceptionHandler*Test`, `ApiErrorCatalogTest`, `TextHelper*Test`, `MessageUcoLab*Test`, `ContentSecurityValidatorTest` | 106 pruebas, 0 fallos |
| `.\mvnw.cmd -B -o clean verify` | BUILD SUCCESS: 4464 pruebas, 0 fallos, 0 errores, 0 omitidas; ArchUnit verde; "All coverage checks have been met" |
| Barrido de literales después del cambio (`new ...Exception("texto")` de 400/403/404/409/501) | 0 sin código. Quedan 8 con código propio ya sembrado (`KPI_001/003/004`, `VCE_003/004/005`, `SEC_001`): el texto sale del catálogo por su código |
| Literales técnicos que se quedan (500 genérico, solo logs) | 91 `CrosscuttingException`, 15 `InternalApplicationException`, 2 `IllegalStateException`, 1 `RuntimeException`, 1 `IdentityProviderException` en Application |
| Compuerta de regresión (`el_codigo_fuente_no_lanza_errores_de_negocio_con_texto_quemado`) | Verde; detecta un `new ValidationException("...")` nuevo |
| Catálogo (`MessageUcoLabCatalogoTest`, fixture regenerado) | 398 códigos, 26 alias sin cambio |

## Deuda registrada (MSG-03)

`src/test/resources/messageucolab/codigos-error-sin-catalogo.txt`: 93 códigos `ERR_*` (y `FIELD_REQUIRED`) de los enums
`*ErrorCode` de Application que ya tenían código propio pero no fila en el catálogo de la BD; su texto sale del respaldo del enum.
La prueba falla si aparece un código nuevo sin catálogo o si alguno de la lista se siembra sin quitarlo de ella. Sembrarlos es el
siguiente paso (no estaba en el alcance de esta oleada).

## Sin cambio de contrato

`ApiErrorResponse.code` y el estado HTTP no cambian para ningún caso migrado (las constantes conservan `VALIDATION_ERROR`,
`FORBIDDEN`, `RESOURCE_NOT_FOUND`, `FEATURE_UNAVAILABLE`, `ERR_CAMPO_OBLIGATORIO`, `ERR_FECHA_HORA_INVALIDA`); las pruebas de
conformidad OpenAPI siguen verdes. Cambia el texto de `message`: ahora es el específico del catálogo en lugar del genérico.
Decisión pendiente de John: dar a estos casos un `code` propio (el frontend podría distinguirlos), lo que sí sería cambio de
contrato y obliga a ajustar la clasificación del frontend (`classifyLoadError`, interceptor 403).
