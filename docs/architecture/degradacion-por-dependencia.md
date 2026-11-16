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
| MinIO (almacenamiento) | Subir y descargar soportes | Subir: **503 `ERR_ALMACENAMIENTO_NO_DISPONIBLE`**, sin registrar metadata. Descargar: **500 `INTERNAL_ERROR`** (pendiente de decisión, ver abajo) | Breaker `fileStorage` | Prueba: `ArchivoAlmacenamientoNoDisponibleTest` (MinIO caído y breaker abierto). Lab (2026-11-15, antes de esta corrección): `asis-datos-minio` detenido → **500 `INTERNAL_ERROR`** al subir |
| SMTP | Envío de correos | La API no cambia: el outbox reintenta y, al agotar los reintentos, marca la notificación como muerta | No (componente `smtp` de `/actuator/health`) | Prueba: `NotificacionOutboxDispatcherTest`, `SmtpHealthIndicatorTest`. Alertas `OutboxNotificacionesMuertas` y `SmtpFallosSostenidos` (Infra) |
| Emisor de identidad (Keycloak) | Iniciar sesión y renovar tokens (lo hace la SPA contra el emisor) | Los tokens vigentes se validan con las claves ya cargadas; un token de una clave nueva no se puede validar (401) | No (componente `emisorIdentidad`) | Prueba: `EmisorIdentidadHealthIndicatorTest`. Código: `NimbusJwtDecoder` con JWKS en caché. Sin medición en vivo |
| Catálogo de mensajes (MessageUcoLab / Azure) | Textos del catálogo | La API responde con el texto de respaldo del enum (mismo significado) | No | Prueba: `MessageUcoLabMessageCatalogAdapterTest` (`error_http_del_componente_degrada_al_fallback`, `error_de_transporte_degrada_al_fallback`) |
| Catálogo de parámetros (Catálogo de Parámetros / Azure) | Lectura de parámetros | Se usa el valor por defecto de `ParametrosPorDefecto` y se escribe un WARN; un obligatorio sin valor por defecto responde error | No | Prueba: `ParametroConValorPorDefectoCatalogAdapterTest` |
| SQL Server | Casi todo | **500 `INTERNAL_ERROR`** (no hay mapeo propio de la caída de la BD) | **Sí** (`db`): la réplica sale del balanceo | Código: `GlobalExceptionHandler` no distingue la caída de la BD. Pendiente: decidir si debe ser 503 y probarlo; no se detuvo SQL Server en el laboratorio porque lo comparten varias personas |

## Cómo se midió en el laboratorio (2026-11-15)

Con un estudiante del conjunto `datos-prueba` se subió un PDF pequeño a `POST /api/v1/archivos/subir`. Antes de cada caso se
llamó hasta obtener 201, para descartar el 503 en frío del PEP. Luego se detuvo el contenedor unos segundos con
`docker stop`, se repitió la carga y se volvió a arrancar. El script queda descrito en
`docs/work-items/VAL-oleada9-backend/VALIDATION.md`.

## Pendientes

- **Descarga con MinIO caído:** sigue en 500. Llevarla a 503 obliga a cambiar
  `DescargarArchivoUseCaseImplTest.storage_no_disponible_no_se_disfraza_de_archivo_inexistente`, que fija que el caso de uso
  propague `StorageUnavailableException`. La traducción no puede ir en `GlobalExceptionHandler` porque la capa de controladores no
  depende de puertos secundarios (`ControllersMustDependOnlyOnInputPortsTest`). Decide el dueño del backend.

- **SQL Server caído:** decidir si responde 503 (por ejemplo, un `DATABASE_UNAVAILABLE` para `CannotGetJdbcConnectionException`)
  y probarlo en un entorno propio.
- **Emisor de identidad:** medir cuánto tiempo siguen validando los tokens con el emisor caído (caché de JWKS).
