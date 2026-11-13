---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# PLAN — VAL-oleada3-mensajes (backend)

Origen: `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`, ítems MSG-05 (mensajes quemados del backend) y MSG-03 (catálogo de
MessageUcoLab al día con la BD y prueba contra el seed). Rama local `fix/VAL-oleada3-mensajes-catalogo` desde `origin/develop`.
Skill aplicada: `uco-catalogos` (puertos neutrales, sin acoplar capas, sin texto inventado ante ausencia).

## Diagnóstico

- 189 `throw new ValidationException/ForbiddenException/ResourceNotFoundException/FeatureUnavailableException("texto")` sin
  código (170 en `application`, 19 en `infrastructure`), más 3 constantes de texto y 12 llamadas a métodos auxiliares que
  recibían el texto (`exigirId`, `IndicadoresRules.requerido`, `ProgramaAcademicoRules.requerido`).
- Hallazgo: con el constructor de texto, la excepción queda con la definición genérica (`VALIDATION_ERROR`, `FORBIDDEN`,
  `RESOURCE_NOT_FOUND`) y `ApiErrorCatalog.from(...)` respondía el texto genérico de esa definición; el texto específico nunca
  llegaba al usuario ni salía del catálogo.

## Criterio: qué es mensaje de usuario y qué se queda literal

| Tipo | Respuesta | Decisión |
| :--- | :--- | :--- |
| `ValidationException`, `ForbiddenException`, `ResourceNotFoundException`, `FeatureUnavailableException` | 400 / 403 / 404 / 501 con el texto en `message` | Mensaje de usuario: pasa al catálogo |
| Las mismas con código propio ya sembrado (`KPI_001/003/004`, `VCE_003/004/005`, `SEC_001`) | El texto sale del catálogo por su código | Se quedan: el literal es solo el respaldo (8 sitios, `DbCodeTranslator` y `DecanoIndicadoresRules`) |
| `CrosscuttingException`, `InternalApplicationException`, `IllegalStateException`, `RuntimeException`, `IdentityProviderException` | 500 con el texto genérico de `INTERNAL_ERROR` | Técnico (guardas de nulos, fallos de proveedor): el texto solo va a los logs; se queda literal (110 sitios) |
| Logs (`LOGGER.*`) | No llegan al usuario | Se quedan literales |

## Diseño (sin cambiar contratos)

- `crosscutting.exception.ErrorDefinition.messageCode()` (método por defecto = `code()`): código con el que se busca el texto en
  el catálogo. Una definición puede conservar el código público de la respuesta y tener su propio código de mensaje.
- `application.exception.catalog.MensajeUsuarioErrorCode` (enum, 111 constantes): el nombre de la constante es el código de
  mensaje; `code()` y `kind()` son los de antes (`VALIDATION_ERROR`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `FEATURE_UNAVAILABLE`,
  `ERR_CAMPO_OBLIGATORIO`, `ERR_FECHA_HORA_INVALIDA`), así el `code` de `ApiErrorResponse`, el estado HTTP y el OpenAPI no
  cambian y el frontend sigue clasificando igual (por ejemplo `FORBIDDEN` → toast de acceso denegado).
  - Códigos nuevos: `VAL_APP_001..075` (400), `ACC_APP_001..017` (403), `NOF_APP_001..014` (404), `DIS_APP_001` (501).
  - Códigos que la BD ya tenía y se reutilizan: `GEN_001`, `PER_001`, `PAG_001`, `KPI_001` (mismo significado).
- Mensajes con datos: `ValidationException/ForbiddenException/ResourceNotFoundException.conParametros(def, args...)`. El texto
  de respaldo y el del catálogo llevan marcadores `{}` que se llenan con los argumentos (`TextHelper.reemplazarMarcadores`).
- `ApiErrorCatalog.fromApplicationException` usa la definición (código, estado y `messageCode`); `FeatureUnavailableException`
  conserva el 501. `GlobalExceptionHandler.resolveErrorMessage` busca el texto por `messageCode` mediante
  `ResolverMensajeUsuarioInputPort` (el controller-advice no toca el puerto secundario) y llena los marcadores. Sin catálogo
  responde el texto de respaldo del enum (mismo significado), nunca un texto inventado. Se conserva la regla de MSG-08: un texto
  de catálogo con `{}` sin resolver no se muestra.
- `SecurityContextAuthenticatedUserResolver`: el principal sin formato UUID responde como "usuario no resuelto" (`ACC_APP_005`);
  `SesionesV2DisabledInterceptor` usa la definición genérica `RESOURCE_NOT_FOUND` (era el mismo texto).

Efecto visible: los 400/403/404 de estos casos ahora muestran su texto específico desde el catálogo en lugar del genérico
("Hay campos con información inválida…", "No tiene permisos…"). El `code` y el estado no cambian.

## MSG-03

- `src/test/resources/messageucolab/catalogo-mensajes-usuario.psv` regenerado con
  `AsistenciasUCO-Infra/components/messages/build-message-catalog.py --previo` desde todos los seeds de la BD: 134 → 398
  códigos (291 que ya estaban en la BD + 107 de MSG-05). Los códigos remotos ya importados se conservan; los 26 alias de
  `application.yml` no cambian.
- Pruebas nuevas:
  - `MensajeUsuarioErrorCodeCatalogoTest`: cada código de `MensajeUsuarioErrorCode` está en el catálogo y sus marcadores
    coinciden; ningún código de error de Application falta del catálogo salvo la deuda registrada en
    `messageucolab/codigos-error-sin-catalogo.txt` (93 `ERR_*` previos, ver VALIDATION); el código fuente no lanza
    errores de negocio con texto quemado (compuerta de regresión).
  - `GlobalExceptionHandlerMensajeCatalogoTest` y `TextHelperMarcadoresTest`.
- `MessageUcoLabCatalogoTest`: 134 → 398.
- Importar el catálogo en una instancia viva de MessageUcoLab (Excel con `--xlsx`) es operación del ambiente; no se hizo desde
  aquí.

## Archivos

- Nuevos: `application/exception/catalog/MensajeUsuarioErrorCode.java`, las tres pruebas y
  `src/test/resources/messageucolab/codigos-error-sin-catalogo.txt`.
- Base: `ErrorDefinition`, `TextHelper`, `ApplicationException`, `BusinessException`, `ValidationException`,
  `ForbiddenException`, `ResourceNotFoundException`, `FeatureUnavailableException`, `ApiErrorDescriptor`, `ApiErrorCatalog`,
  `GlobalExceptionHandler`.
- 105 archivos de casos de uso, reglas, mappers, controllers y repositorios con el reemplazo del texto por su código.

## Dependencias entre repositorios

- BD: migración 41 y semilla `62_val_mensajes_aplicacion.sql` (rama `fix/VAL-oleada3-mensajes-catalogo` de la BD). Sin esas
  filas el backend responde el texto de respaldo.
- Infra: `build-message-catalog.py` con `--previo` y los tipos `VALIDATION_ERROR`, `SECURITY_ERROR`, `TECHNICAL_ERROR`, `INFO`.
