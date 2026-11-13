---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# PLAN — VAL-oleada2-cumplimiento (backend, agente B)

Origen: `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`. Rama local `fix/VAL-oleada2-cumplimiento`, montada sobre
`fix/VAL-oleada1-urgentes`. Decisiones de John del 2026-11-12: **A** (se puede tocar código de Sergio y José Alejandro)
y **G** (agregar Bean Validation con `@Valid` donde falta).

## ROL-06 — Bean Validation (decisión G)

| Qué | Cambio |
|---|---|
| Dependencia | `spring-boot-starter-validation` (versión del BOM de Spring Boot 4.0.8; Hibernate Validator 9.0.1). |
| Controladores | `@Valid` en todos los `@RequestBody` con request tipado (31 controladores). Sin request tipado, a propósito: `SesionV2Controller` (`Map`, lo valida `SesionV2RequestParser`), `AzureEventGridWebhookController` (`List<Map>`, contrato de Azure) y `RealtimeEventsController` (`Map`). |
| Requests | Anotaciones `jakarta.validation` solo en `infrastructure/adapter/primary/controller/**`, copiadas de las reglas simples de cada validator propio (mismo mensaje): obligatoriedad, longitud de la columna, formato de correo, token y PIN, rangos. Cascada con `@Valid` en `facultadNueva`, `programaNuevo`, `registros` (asistencias y toma). |
| Los siete controladores sin validador | `TomaAsistenciaController` (lote 1..200, cada registro con estudiante y estado), `SesionTomaController` (`@Valid`; el modo vacío conserva `TOM_002` en Application), `AsignaturaPlanController` (nuevo `AsociarAsignaturaPlanRequest` en Infrastructure; antes recibía el DTO de Application y un campo nulo terminaba en `CrosscuttingException`), `CoordinadorPortalController` (`inp` obligatorio y positivo; asignatura con las longitudes de `Asignatura`, `Area`, `Componente` y `CaracterAsignaturaPlan`), `DocenteGrupoEstudianteController` (motivo hasta 500; el motivo ausente lo sigue rechazando Application), `VerificacionCorreoController` (token hasta 256), `SesionV2Controller` (documentado: `Map` + parser). |
| Contrato de error | `GlobalExceptionHandler` atiende `MethodArgumentNotValidException` aparte: 400 `VALIDATION_ERROR` con `details[]` (`field`, `code`, `message`), igual que `RequestValidationException`. Antes salía como `INVALID_REQUEST` sin detalles. `BeanValidationFieldErrorMapper`: un error por campo, orden de declaración del request, solo el `message` explícito (nunca el texto del proveedor). `BeanValidationCodes` (payload) fija el código cuando el validator propio usa otro (p. ej. documento no positivo = `FIELD_INVALID_FORMAT`). |
| UUID nulo | `@NonEmptyUuid` (restriccion propia en `controller/validation`, misma regla que `ValidationHelper.isNonEmptyUuid`): `FIELD_INVALID_UUID` por defecto, `FIELD_REQUIRED` con payload donde el validator propio lo trata como ausente. |
| Pruebas existentes ajustadas | `MatriculaQrControllersTest` (el rechazo previo al caso de uso puede llegar por Bean Validation o por el validator propio), `FileProvidersConfigurationTest` (los puertos se publican envueltos en su breaker), `MinioClientTimeoutBoundaryTest` y `ConsultasTransversalesWiringConfigurationTest` (firmas nuevas del Composition Root), `RealtimeStreamErrorHttpTest` (espera a que llegue el fragmento `data:` del evento; antes leia a medias) y `SecurityConfigTest` (pruebas nuevas). |
| Validators propios | Se conservan todos. Bean Validation es la primera barrera; `BeanValidationPatterns` tolera espacios alrededor y vacío en opcionales para no rechazar lo que el validator propio acepta. |
| Documentos | `docs/architecture/input-validation.md` (sección Bean Validation) y `docs/testing/backend-validation-guide.md` ya no dicen que el proyecto no usa Jakarta Validation. |

## F12-09 y OBS-10 — desechabilidad y sondas

- `server.shutdown: graceful` y `spring.lifecycle.timeout-per-shutdown-phase: ${SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE:20s}`.
- `management.endpoint.health.probes.enabled: true`; grupo `liveness` = `livenessState`; grupo `readiness` =
  `readinessState,db,circuitBreakers`. `db` es el indicador de la BD (DataSource). `circuitBreakers` es el indicador de
  Resilience4j con el estado de los breakers del PEP, MinIO y ClamAV (sin llamadas de red extra ni acoplar capas). Un
  breaker abierto sale como `CIRCUIT_OPEN`, que no tumba la readiness (con el PEP caído todas las réplicas fallan cerrado;
  sacarlas del balanceo no restaura el servicio).
- `BE/Dockerfile`: `HEALTHCHECK` con `wget` de BusyBox contra
  `http://127.0.0.1:${MANAGEMENT_SERVER_PORT:-8090}/actuator/health/readiness` (Actuator escucha en loopback dentro del
  contenedor). El `ENTRYPOINT` sigue en forma exec: la JVM recibe SIGTERM.
- `management.metrics.distribution.percentiles-histogram.http.server.requests: true` (pedido del agente I para la alerta
  `LatenciaP95Alta`).
- Métricas de fallos de notificaciones (pedido del coordinador): ya existían, no se agregó nada. Outbox:
  `notification_dispatch_total{outcome="retry"|"dead"|"lost_lease"}` (y `delivered`); SMTP:
  `notification_email_send_total{outcome="retry"|"permanent"|"no_recipients"}` (y `sent`).

## REA-02 — resiliencia (Resilience4j)

- `io.github.resilience4j:resilience4j-spring-boot4:2.4.0` (la línea para Spring Boot 4; `-spring-boot3` no aplica a
  Boot 4.0.8). Solo registros configurados desde `application.yml`; sin anotaciones AOP.
- `resilience4j.circuitbreaker.configs.default` + instancias `securityBaselinePep`, `fileStorage` y `malwareScan`, todo
  con variables de entorno `RESILIENCE_CB_*` (ventana, mínimo de llamadas, umbral de fallos y de llamadas lentas, espera
  en abierto, llamadas en semiabierto).
- Adaptadores secundarios (no el dominio):
  - `SecurityBaselinePepClient`: con el breaker abierto devuelve `UNAVAILABLE` sin llamar al PEP (el filtro sigue en
    503, fail-closed, sin esperar los 5 s del `request-timeout`). Solo `UNAVAILABLE` cuenta como fallo; 204/401/403 son
    respuestas sanas.
  - `CircuitBreakerFileStorageAdapter` decora el `FileStoragePort` de MinIO: abierto lanza `StorageUnavailableException`;
    `ObjectNotFoundException` no cuenta como fallo.
  - `CircuitBreakerMalwareScanAdapter` decora el `MalwareScanPort` de ClamAV: abierto lanza `MalwareScanException`
    (nunca `CLEAN`); `INFECTED` no cuenta como fallo.
- Timeout: cada cliente ya tenía el suyo en el transporte (PEP `request-timeout` 5 s y `connect-timeout` 2 s; MinIO
  connect/write/read; ClamAV connect/read). Para llamadas bloqueantes es el timeout correcto (un `TimeLimiter` no
  interrumpe un socket); el breaker evita repetir ese timeout mientras la dependencia está caída y además mide las
  llamadas lentas (`slow-call-duration-threshold`).

## PAR-05 y PAR-06 — valores quemados

- PAR-05 (backend): `SubirArchivoUseCaseImpl` lee `ARCHIVO/TAMANIO_MAXIMO_BYTES` vía `ParameterCatalogPort` y
  `ContentSecurityValidator` valida con ese límite (mensaje con el tamaño real). 5 MiB queda como techo técnico y
  respaldo: es el límite multipart del servidor y el máximo que admite el catálogo para ese parámetro (262144..5242880).
- PAR-05 (frontend): opción **endpoint**. Ya existía `GET /api/v1/configuracion/reclamos`, que publica
  `tamanioMaximoBytes` desde el mismo parámetro; el FE ya lo cargaba pero validaba con 5 MB fijo. Ahora
  `validateSoporteFile(file, tamanioMaximoBytes)` usa ese valor y 5 MB solo es respaldo si la configuración no llegó. No
  hizo falta devolver el límite en el error del backend.
- PAR-06 (auditoría): `ConsultasTransversalesUseCaseImpl` lee `AUDITORIA/RANGO_MAXIMO_DIAS` (1..366) con respaldo de 90
  días. **El parámetro no existe en la BD**; ver la propuesta de migración en `VALIDATION.md`.
- PAR-06 (consolidado del coordinador, FE): el 15 % fijo ya tenía parámetro (`ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA`, 15)
  y el backend lo publica en los KPI del coordinador y del decano (`umbralAdvertencia`, `umbralPerdida`). El consolidado
  los lee de ahí y los pasa al semáforo; 15/20 quedan como respaldo.

## CF-05 y CF-06 — encabezados y CORS

- `SecurityHttpHeaders` (aplicado en `SecurityConfig`): CSP de la API `default-src 'none'; frame-ancestors 'none';
  base-uri 'none'; form-action 'none'`; CSP propia para Swagger UI (`'self'`, estilos en línea e imágenes `data:`);
  `X-Frame-Options: DENY`; `X-Content-Type-Options: nosniff`; `Referrer-Policy: no-referrer`; `Permissions-Policy`;
  HSTS de un año con subdominios, solo en HTTPS y apagable con `APP_SECURITY_HSTS_ENABLED=false`.
- CORS: se agrega la prueba de preflight de origen rechazado sin encabezados CORS (403) junto a las existentes de origen
  permitido.

## F12-11 — logs a stdout

- `logging.file.name: ${ASISTENCIAS_UCO_LOG_FILE:}`: sin la variable no hay archivo; `logging.structured.format.console:
  ${ASISTENCIAS_UCO_LOG_CONSOLE_FORMAT:logstash}` (JSON por stdout). Los perfiles `local` y `dev` vuelven a activar el
  archivo `infra/observability/logs/asistencias-uco.jsonl` para el Alloy local.
- `docs/architecture/infrastructure-structure.md` actualizado.
- La línea que lo contradecía está en INF (`docs/architecture/arquitectura-integral.md:331`), fuera de este repo: queda
  anotada para el agente I. Con este cambio la parte de stdout ya es cierta; la parte "recolectadas por OTLP" sigue sin
  serlo (los logs no se exportan por OTLP: `management.logging.export.otlp.enabled: false`).

## Dependencias con CVE (pedido del coordinador, Trivy)

Transitivas de `io.minio:minio:9.0.3` fijadas en `dependencyManagement`: `com.google.guava:guava` 33.7.2-jre
(CVE-2026-102554) y `org.xerial.snappy:snappy-java` 1.1.10.12 (CVE-2026-108106, CVE-2026-93451; 1.1.10.12 es la que
declara minio 9.0.4 y supera el minimo 1.1.10.9). No se subio minio a 9.0.4 porque ademas mueve okhttp (5.5.1), Bouncy
Castle (1.86) y Jackson 2 (2.21.8), que el pom alinea a proposito. `mvnw dependency:tree` confirma guava 33.7.2-jre y
snappy-java 1.1.10.12.

## Fuera de alcance

BD (compuerta en curso), repo INF, `.github/`, FE fuera de lo necesario para PAR-05/06.

## Cadena de suministro y CI (agente T)

TRI-01, TRI-02, TRI-06, TRI-07, SEC-05, SEC-08, EST-09, PRU-04 y espera del quality gate de Sonar: plan, cambios, triage y validación en [CI-CADENA-SUMINISTRO.md](CI-CADENA-SUMINISTRO.md).
