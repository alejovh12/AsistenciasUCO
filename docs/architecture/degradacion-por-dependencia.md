---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# Comportamiento con una dependencia caída (REA-02)

Qué responde el backend cuando una dependencia externa no está disponible. Regla general: **la caída de una dependencia
se publica como 503 con un código del catálogo, nunca como 500 genérico, y nunca abre acceso ni guarda datos a medias**
(fallar cerrado). Columna "Evidencia": **Lab** = medido contra el laboratorio; **Prueba** = prueba automática; **Código** =
deducido del código, sin prueba propia (pendiente).

| Dependencia | Qué deja de funcionar | Respuesta de la API | `readiness` | Evidencia |
| :--- | :--- | :--- | :--- | :--- |
| PEP / PDP (Security Baseline) | Toda ruta protegida | **503 `ACCESS_DECISION_UNAVAILABLE`**; nunca permite por defecto | Breaker del PEP en `circuitBreakers` | Prueba: `SecurityPepEnforcementFilterTest`, `SecurityBaselinePepCircuitBreakerTest`. Lab (2026-11-15): la primera llamada en frío devolvió este 503 (ver REA-01 sobre la latencia del PDP) |
| ClamAV (antivirus) | Subir soportes | **503 `ERR_ANTIVIRUS_NO_DISPONIBLE`**; no guarda bytes ni metadata | Breaker `malwareScan` | Prueba: `SubirArchivoAntivirusNoDisponibleTest`. **Lab (2026-11-15): `asis-antivirus-clamav` detenido → 503 con ese código** |
| MinIO (almacenamiento) | Subir y descargar soportes | Subir: **503 `ERR_ALMACENAMIENTO_NO_DISPONIBLE`**, sin registrar metadata. Descargar: **503 `ERR_ALMACENAMIENTO_NO_DISPONIBLE`** (`DescargarArchivoUseCaseImpl` traduce la falla, igual que al subir) | Breaker `fileStorage` | Prueba: `ArchivoAlmacenamientoNoDisponibleTest` (subir) y `DescargarArchivoAlmacenamientoNoDisponibleTest` (descargar), con MinIO caído y breaker abierto. Lab (2026-11-15, antes de esta corrección): `asis-datos-minio` detenido → **500 `INTERNAL_ERROR`** al subir |
| SMTP | Envío de correos | La API no cambia: el outbox reintenta y, al agotar los reintentos, marca la notificación como muerta | No (componente `smtp` de `/actuator/health`) | Prueba: `NotificacionOutboxDispatcherTest`, `SmtpHealthIndicatorTest`. Alertas `OutboxNotificacionesMuertas` y `SmtpFallosSostenidos` (Infra) |
| Emisor de identidad (Keycloak) | Iniciar sesión y renovar tokens (lo hace la SPA contra el emisor) | Los tokens vigentes se validan con las claves ya cargadas; un token de una clave nueva no se puede validar (401) | No (componente `emisorIdentidad`) | Prueba: `EmisorIdentidadHealthIndicatorTest`. Código: `NimbusJwtDecoder` con JWKS en caché. Sin medición en vivo |
| Catálogo de mensajes (MessageUcoLab / Azure) | Textos del catálogo | La API responde con el texto de respaldo del enum (mismo significado) | No | Prueba: `MessageUcoLabMessageCatalogAdapterTest` (`error_http_del_componente_degrada_al_fallback`, `error_de_transporte_degrada_al_fallback`) |
| Catálogo de parámetros (Catálogo de Parámetros / Azure) | Lectura de parámetros | Se usa el valor por defecto de `ParametrosPorDefecto` y se escribe un WARN; un obligatorio sin valor por defecto responde error | No | Prueba: `ParametroConValorPorDefectoCatalogAdapterTest` |
| SQL Server | Casi todo | **503 `ERR_BD_NO_DISPONIBLE`** con `Retry-After: 60` cuando no hay conexión (rechazada, sin ruta, timeout del pool Hikari, SQLSTATE 08xxx). Un error ordinario de SQL (restricción, deadlock, rechazo de negocio de un SP) **no** se mapea: sigue su respuesta de antes | **Sí** (`db`): la réplica sale del balanceo | Prueba: `GlobalExceptionHandlerBaseDatosNoDisponibleTest`. Sin medición en vivo: no se detuvo SQL Server en el laboratorio porque lo comparten varias personas |

## Cómo se midió en el laboratorio (2026-11-15)

Con un estudiante del conjunto `datos-prueba` se subió un PDF pequeño a `POST /api/v1/archivos/subir`. Antes de cada caso se
llamó hasta obtener 201, para descartar el 503 en frío del PEP. Luego se detuvo el contenedor unos segundos con
`docker stop`, se repitió la carga y se volvió a arrancar. El script queda descrito en
`docs/work-items/VAL-oleada9-backend/VALIDATION.md`.

## SQL Server caído (detalle)

`GlobalExceptionHandler` recorre la cadena de causas (máximo 12 niveles, tolera ciclos) en `handleUnexpected`, `handleTechnical` y
`handleApplication` (esta última solo si iba a responder 500) y trata como caída: `CannotGetJdbcConnectionException`,
`DataAccessResourceFailureException`, `JDBCConnectionException` (Hibernate), `SQLTransientConnectionException` (incluye el timeout de
conexión de Hikari), `SQLNonTransientConnectionException`, `SQLRecoverableException` y cualquier `SQLException` con SQLSTATE `08`.
`CannotCreateTransactionException` cuenta solo si su causa es de conexión. El código vive en `DependenciaErrorCode.ERR_BD_NO_DISPONIBLE`
(alias remoto `ERR122`; texto sembrado en la migración 67 de BD). Como el catálogo de mensajes vive en la misma BD, con ella caída
el texto sale del respaldo del enum (mismo significado). Se registra una sola línea WARN (`Base de datos no disponible`) con
`correlationId`, ruta y clase de la causa, sin traza; la respuesta conserva `correlationId` en el cuerpo.

## Pendientes

- **Emisor de identidad:** medir cuánto tiempo siguen validando los tokens con el emisor caído (caché de JWKS).
- **SQL Server caído:** medirlo en un entorno propio (hoy solo hay prueba automática).

### Tiempo de espera con SQL Server caído

Con la BD apagada la respuesta 503 llega tras la espera por una conexión del pool, no antes. Por defecto (Hikari) eran 30 s; ahora
son **5 s** y se ajustan por variable:

| Variable | Defecto | Efecto |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | `5000` (ms; mínimo de Hikari 250) | Espera máxima de una petición por una conexión; al vencer, `SQLTransientConnectionException` → 503 `ERR_BD_NO_DISPONIBLE`. |
| `SPRING_DATASOURCE_LOGIN_TIMEOUT_SECONDS` | `5` (s) | `loginTimeout` del driver (`hikari.data-source-properties`), para que un intento de conexión colgado no supere el plazo del pool. |

No hay bucle de reintentos que multiplique la espera: `open-in-view` está apagado, cada petición obtiene la conexión una sola vez al abrir su
transacción, y el único reintento de la capa de aplicación (registro por QR, hasta 3 veces) solo aplica a interbloqueos SQL 1205, no a
conexiones caídas. Peor caso por petición: el plazo configurado. Subirlo solo si el arranque o los picos de latencia de red lo exigen.
Prueba: `OperationalReadinessConfigurationTest.f12_08_pool_de_conexiones_dimensionado_por_variable_y_por_replica`. Sin medición en vivo
(SQL Server compartido).
