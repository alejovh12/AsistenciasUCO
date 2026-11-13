---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VAL-pendientes-rea-msg (Backend) - PLAN

Fecha: 2026-11-13. Rama local `fix/VAL-pendientes-rea-msg` desde `origin/develop` (29d66aaf, oleada 3 unida). Origen: hallazgos
H2 y H3 de la prueba de carga ([VAL-oleada3-carga/VALIDATION.md](../VAL-oleada3-carga/VALIDATION.md) §3) y la deuda MSG-03 de
[VAL-oleada3-mensajes](../VAL-oleada3-mensajes/PLAN.md) (93 `ERR_*` sin fila de catálogo). Filas de la lista de chequeo:
OBS-08, REA-02, REA-06, MSG-03 y MSG-05 (la lista no se edita desde aquí). Pareja en la BD: rama del mismo nombre en
`gestion-asistencia-db`, migración 42.

## H2 (OBS-08): `/actuator/prometheus` respondía 401 en el puerto de gestión

- AS-IS: Actuator sale por el puerto de gestión (`MANAGEMENT_SERVER_PORT`, 8090, loopback). Spring Boot registra la misma
  `springSecurityFilterChain` en el contexto de gestión, así que la cadena de la API (`SecurityConfig`) exigía Bearer para
  `/actuator/prometheus`. El scrape de Infra (`components/observability/prometheus/prometheus.yml`, job `asistencias-backend`,
  `host.docker.internal:8090`, `metrics_path: /actuator/prometheus`, sin credenciales) recibía 401.
- Cambio: `infrastructure/config/management/ManagementEndpointsSecurityConfiguration` agrega una `SecurityFilterChain` con
  `@Order(HIGHEST_PRECEDENCE)` cuyo `securityMatcher` es `ManagementPortRequestMatcher`: solo coincide cuando la petición entró
  por el puerto de gestión real (`local.management.port`) y ese puerto es distinto del de la API. Ahí permite `GET` a
  `/actuator/health`, `/actuator/health/**` y `/actuator/prometheus` y niega todo lo demás (403). La cadena de la API no cambia.
- Infra: target, puerto y ruta coinciden con lo que expone el backend; **no se cambia Infra**. Las sondas de Traefik
  (`/actuator/health/readiness` en 8090/8092) siguen funcionando.
- Sin cambios de `application.yml` para H2 (`include: health,prometheus` y loopback se conservan).

## H3 (REA-02 / REA-06): ClamAV caído respondía 500

- AS-IS: `SubirArchivoUseCaseImpl` convertía `MalwareScanException` (conexión rechazada, timeout, respuesta inválida o breaker
  `malwareScan` abierto) en `InternalApplicationException` → 500 `INTERNAL_APPLICATION_ERROR`.
- Cambio:
  - `ErrorKind.SERVICE_UNAVAILABLE` (nuevo) → `ApiErrorCatalog` lo publica como 503.
  - `application/exception/internal/ServiceUnavailableException` (subclase de `InternalApplicationException`; solo acepta
    definiciones `SERVICE_UNAVAILABLE`, conserva la causa).
  - `application/features/archivo/exception/ArchivoErrorCode.ERR_ANTIVIRUS_NO_DISPONIBLE` (código público y de catálogo).
  - El caso de uso lanza esa excepción. Sigue fallando cerrado: no se guardan bytes ni metadata. El decorador del circuit breaker
    no cambia (mismas reglas de conteo y apertura).
- Respuesta: `503` con `ApiErrorResponse` estándar (`status`, `error`, `code=ERR_ANTIVIRUS_NO_DISPONIBLE`, `message` del
  catálogo, `path`, `correlationId`). Se registra con el log de error interno (nivel ERROR con causa), igual que antes.
- El reclamo (`POST /api/v1/asistencias/revisiones`) no llama al antivirus; solo la subida (`POST /api/v1/archivos/subir`).
- Contrato: fila de `POST /api/v1/archivos/subir` en `docs/contracts/HTTP_AS_IS_MATRIX.md` actualizada. La subida no está en
  el OpenAPI golden path, así que no se toca el YAML.

## MSG-03: 93 `ERR_*` sin catálogo

- La lista estaba en `src/test/resources/messageucolab/codigos-error-sin-catalogo.txt` (93 códigos, deuda registrada por
  `MensajeUsuarioErrorCodeCatalogoTest`). La BD los siembra en la migración 42 (`42_val_mensajes_err_pendientes_up/down.sql`,
  semilla `63_val_mensajes_err_pendientes.sql`), más `ERR_ANTIVIRUS_NO_DISPONIBLE`: 94 filas en `CatalogoMensajeUsuario` y
  `CatalogoMensajeTecnico`, textos con tildes.
- `FIELD_REQUIRED` (constante `GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA`) es un código genérico de campo; para no reservarlo a
  un texto de matrícula, la constante conserva el código público `FIELD_REQUIRED` y busca su texto por
  `messageCode() = ERR_CORREO_REQUERIDO_MATRICULA` (mismo mecanismo de MSG-05).
- `ERR_DOCENTE_REQUERIDO` existe en `DocenteErrorCode` y `SesionErrorCode` con textos de respaldo distintos; el catálogo tiene uno
  solo ("El docente es obligatorio.").
- Fixture `catalogo-mensajes-usuario.psv` regenerado con `AsistenciasUCO-Infra/components/messages/build-message-catalog.py
  --previo` (398 → 492). Los 26 alias anteriores de `app.providers.messageucolab.code-aliases` conservan su código remoto; se
  agregan 94 (`ERR025`..`ERR118`).
- `codigos-error-sin-catalogo.txt` queda vacío (solo comentarios): la prueba existente
  `ningun_codigo_de_error_de_aplicacion_falta_del_catalogo_salvo_la_deuda_registrada` ahora exige catálogo para todos los
  códigos de los enums `ErrorDefinition` de Application.

## Pruebas (RED → GREEN)

| Prueba | Cubre | RED observado |
| :--- | :--- | :--- |
| `ManagementPortActuatorSecurityTest` (Tomcat real en dos puertos aleatorios, cadena real de la API) | Prometheus 200 en gestión, health 200, API 401 en `/actuator/prometheus`, otras rutas/POST de gestión 403 | Sin la cadena nueva: `expected 200 but was 401` y `expected 403 but was 401` |
| `ManagementPortRequestMatcherTest` | puerto de gestión ausente, igual al de la API o distinto | — (unidad nueva) |
| `SubirArchivoAntivirusNoDisponibleTest` (ClamAV real por socket + breaker real + controlador y manejador reales) | conexión rechazada, timeout de lectura, breaker abierto, fallos que abren el breaker, respaldo sin catálogo; nada se guarda | Con el código anterior: 5 de 5 `Status expected 503 but was 500` |
| `ServiceUnavailableExceptionTest` | causa, tipo 503 obligatorio, `messageCode` de la matrícula sin correo | — |
| `MensajeUsuarioErrorCodeCatalogoTest` (existente) | ningún código de Application sin catálogo, deuda vacía | — (compuerta ya existente; ahora sin deuda) |
| `MessageUcoLabCatalogoTest` (existente, conteos 492 y 120) | fixture y alias consistentes | — |

## Fuera de alcance / pendientes

- H1, H4 y H5 (caché del PDP, timeout del PEP, ventana de réplica) no se tocan.
- Importar el catálogo nuevo a una instancia viva de MessageUcoLab (Excel `--xlsx`) es operación del ambiente.
- La migración 42 no se aplica a la BD compartida de desarrollo hasta la unión.
