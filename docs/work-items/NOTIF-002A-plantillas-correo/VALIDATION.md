---
status: validated-local
type: validation
scope: database-backend
last-reviewed: 2026-10-30
---

# VALIDATION — NOTIF-002A: Plantillas de notificación y envío de correo SMTP

## Dictamen

**VALIDADO EN LOCAL** (sin push ni PR). Fases 0 a 6 completas, incluida la fase 4b (modo de pruebas con redirección, D15). Tras el merge con `origin/develop` (2026-10-30): unitarias 2.864/0 fallos; integración igual a la línea base salvo +11 IT de NOTIF-002A en verde; gate de BD PASS; E2E-M01..M09 en verde (9/9).

La implementación de las fases 1 a 5 la hizo el agente implementador (Antigravity) bajo revisión por fases; la fase 6 (E2E) y esta validación posterior al merge las ejecutó el revisor (Claude) tras agotarse la cuota del implementador. Todas las cifras de este documento se midieron en la corrida indicada; ninguna se tomó de otra.

## Comandos ejecutados

| Fecha / entorno | Comando | Resultado | Cifras |
|---|---|---|---|
| 2026-10-30, BD Docker local, rama `feature/NOTIF-002A-plantillas-correo` (DB, fusionada con develop `bf6e3f1`) | `deploy_schema.ps1` + `test_summary.ps1` | **DB GATE PASS** | `TOTAL_EXECUTED=376`, `PASSED=375`, `FAILED=0`, `SKIPPED=1` (permitida), `SQL_ERROR_COUNT=0`, `UNAUTHORIZED_SKIPS=0`, `@@TRANCOUNT=0` |
| 2026-10-30, Java 25, rama backend (HEAD `e21e5ea`) | `mvnw.cmd test jacoco:report` | **PASS** | `Tests run: 2864, Failures: 0, Errors: 0, Skipped: 0` |
| 2026-10-30, BD Docker local, entorno cargado sin BOM, rama backend | `mvnw.cmd -Pintegration failsafe:integration-test failsafe:verify` | FAIL preexistente | `Tests run: 258, Failures: 1, Errors: 1, Skipped: 7`; `NotificacionOutboxIT` 9/9; `NotificacionSmtpGreenMailIT` 11/11 |
| 2026-10-30, backend en `:8090` con sumidero SMTP local | `scratch/test_notif002a.ps1` (E2E-M01..M09) | **PASS** | 9/9 pruebas en verde (ver §E2E) |

### Línea base (Fase 0, mismo entorno, antes de tocar código)

| Capa | Resultado |
|---|---|
| Unitarias | 2.797 pruebas, 0 fallos |
| Integración (failsafe) | 247 ejecutadas, 1 fallo, 1 error, 7 omitidas |
| Gate de BD | 392 ejecutadas, 391 pasadas, 1 omitida permitida |

**Diferencias frente a la línea base:**
- **Integración:** 258 = 247 + 11 (`NotificacionSmtpGreenMailIT`, `IT-E01..IT-E11`). Los dos fallos son los mismos preexistentes y dependen de los datos de la BD local, no de notificaciones: `CoreViewQueriesJpaParityIT:185` (fixture con 4 estudiantes; se exigen ≥ 11) y `AuthorizationReportJpaParityIT:102` (UUID nulo).
- **Gate de BD:** 376 frente a 393 antes del merge. La diferencia (−17) viene de que `origin/develop` eliminó 6 archivos de prueba de procedimientos sin uso (`test_usp_consultar_catalogos_y_revisiones_paginado`, `…docentes_paginado`, `…estudiantes_grupo_paginado`, `…estudiantes_paginado`, `…registrar_docente_en_grupo_usuario_no_existente`, `…resolver_solicitud_matricula`). Todas las pruebas de NOTIF-001 y NOTIF-002A siguen en el gate y en verde (`DB-P01..P05`, `DB-C01..C04`, `DB-M01..M02`, `DB-A01`, `DB-G01..G02`).
- **Unitarias:** 2.864 = 2.797 + 67 pruebas nuevas de NOTIF-002A (plantillas, contacto, SMTP, tope, modo de pruebas, wiring, ArchUnit, casos de uso emisores).

## Cobertura (JaCoCo, corrida de 2.864 pruebas)

| Alcance | Líneas | Instrucciones | Ramas |
|---|---|---|---|
| Global backend | 14.617 / 15.527 (94,14 %) | 72.576 / 76.624 (94,72 %) | 4.010 / 4.761 (84,23 %) |
| Módulo `notification` completo (outbox, plantillas, contacto, SMTP) | 1.029 / 1.043 (98,66 %) | 4.515 / 4.575 (98,69 %) | 374 / 405 (92,35 %) |

Metas del TEST_PLAN para código nuevo (≥ 90 % líneas, ≥ 85 % ramas): cumplidas por el módulo.

## E2E (puerto 8090)

**Declaración:** ejecutado con `APP_ADAPTERS_SECURITY_PROVIDER=legacy` y `APP_ADAPTERS_IDENTITY_PROVIDER=legacy`. **NO se probó contra el PDP remoto de seguridad baseline.** Backend propio en `:8090` (no se tocaron `:8080` ni `:4200`), JWT reales de Keycloak, BD local `gestionasistenciadb`. **No se usó Mailpit ni imágenes Docker de terceros:** el SMTP fue un sumidero local basado en GreenMail (`scratch/GreenMailSink.java`, no versionado) en `127.0.0.1:3025` que vuelca cada mensaje como `.eml` en una carpeta temporal fuera del repositorio. Salida cruda de la corrida final fuera del repositorio.

| ID | Resultado | Evidencia |
|---|---|---|
| E2E-M01 | PASS | Arranque con `sender=log` (sin beans SMTP) y con `sender=smtp`, ambos `UP` |
| E2E-M02 | PASS | Retiro HU051 → HTTP 200, fila `ENTREGADA`, asunto y cuerpo de la plantilla v1, 3 destinatarios en BCC (estudiante, docente, coordinador) |
| E2E-M03 | PASS | El motivo del retiro no aparece en el cuerpo ni en el `.eml` (D12) |
| E2E-M04 | PASS | `multipart` con `text/plain` y `text/html`, layout fijo presente |
| E2E-M05 | PASS | Plantilla v2 activa aplicada tras el TTL de caché; semilla v1 restaurada |
| E2E-M06 | PASS | Sumidero detenido → retiro HTTP 200, fila `FALLIDA_REINTENTABLE` (intentos ≥ 1, código `FALLO_REINTENTABLE`) → `ENTREGADA` al volver el sumidero |
| E2E-M07 | PASS | `notification_email_send_total` y `notification_template_render_total` presentes en `/actuator/prometheus` |
| E2E-M08 | PASS | `NotificacionSaliente`=0, 4 plantillas v1 activas, matrícula `A`, carpeta temporal eliminada |
| E2E-M09 | PASS | Modo de pruebas con 2 direcciones ficticias: un único correo con BCC solo a esas dos, asunto `[PRUEBA]`, aviso con N=3, **ninguna** dirección real en el `.eml`, `WARN` de arranque sin direcciones y `notification_email_redirected_total` ≥ 1 |

Totales: 9 ejecutadas, 9 en verde, 0 omitidas. El E2E automatizado no usó un SMTP real (el servidor SMTP de la universidad aún no se entrega, PLAN Q4). La entrega real se validó aparte (siguiente sección).

### Envío real con Gmail en modo de pruebas (2026-10-30, validado por el owner)

Backend propio en `:8090` (seguridad e identidad `legacy`), `OUTBOX_NOTIFICATION_SENDER=smtp` hacia `smtp.gmail.com:587` con STARTTLS, contraseña de aplicación de una cuenta de pruebas del owner (leída del `.env` local ignorado por git; no se registra en ningún log) y `SMTP_NOTIFICATION_TEST_MODE_ENABLED=true` con `REDIRECT_TO` igual a la cuenta de pruebas. Disparador: retiro HU051.

| Comprobación | Resultado |
| :-- | :-- |
| Advertencia de arranque del modo de pruebas (sin direcciones) | Presente |
| Retiro HU051 | HTTP 200 |
| Log del sender | `Notificacion enviada por SMTP ... tipo=RETIRO_ESTUDIANTE_GRUPO, destinatarios=3, origenPlantilla=db, outcome=sent` |
| Fila del outbox | `ENTREGADA` |
| Buzones que recibieron el correo | Solo la cuenta de pruebas (los 3 destinatarios reales omitidos) |
| Recepción en la bandeja de la cuenta de pruebas, con asunto `[PRUEBA]` y aviso inicial | **Confirmada por el owner** |

Restauración tras la prueba: outbox 0 filas, matrícula del estudiante de prueba en `A`, 4 plantillas v1 activas, puerto 8090 libre. Pendiente de seguridad: revocar la contraseña de aplicación al terminar las pruebas.

### Ajustes al script de E2E durante la validación (scratch, no versionado)

El E2E falló cuatro veces por errores del propio script, no del sistema, y se corrigieron sin tocar el código de producción: (1) cadenas con tildes en un `.ps1` sin BOM (se guardó con BOM UTF-8); (2) la plantilla v2 de M05 usaba columnas y marcadores inexistentes (`asuntoPlantilla`, `{{grupo}}`) en lugar de `asunto`, `cuerpo`, `creadoEn` y `{grupo}`; (3) `sqlcmd` sin `QUOTED_IDENTIFIER ON`, necesario por el índice único filtrado; (4) expectativas erróneas en M06 (`PENDIENTE|SEND_ERROR`; el contrato de NOTIF-001 deja la fila en `FALLIDA_REINTENTABLE|FALLO_REINTENTABLE`), en M07/M09 (se leyeron las métricas por Prometheus, como en NOTIF-001) y en el texto del `WARN` de M09 (que coincide con CONTRACT §4.6).

## Tests existentes modificados (para el PR)

| Archivo | Cambio | Commit | Justificación |
|---|---|---|---|
| `NotificationWiringTest.java` (`WI08_sender_desconocido_falla`) | El valor `smtp` pasa a `desconocido` (única línea) | `bc32423` | `smtp` pasa a ser un valor válido del enum; el test habría pasado por una razón equivocada. CONTRACT §10.3 |
| `NotificacionOutboxDispatcherTest.java` | +1 línea en `setUp()`: `when(sender.disponible()).thenReturn(true);`, y 2 métodos nuevos al final (`BE_K04`, `BE_K05`) | `7df2d8e` (+ `7d13f2a` los métodos nuevos) | El SPI `NotificacionChannelSender` ganó `disponible()` con valor por defecto y Mockito no ejecuta los métodos `default`. CONTRACT §10.4 |
| `test_suite.sql` (DB) | Registro aditivo de `:r /tmp/test/test_notif002a_plantillas_contacto.sql` | `66d63d0` | Registro de la prueba nueva (TEST_PLAN §1) |

Aviso para quien revise el PR: `DbExceptionTranslatorTest` y `DbFailureClassifier` aparecen modificados en el diff porque llegaron del merge con `origin/develop` (commit `f5dcb56`), no de NOTIF-002A.

## Efectos colaterales conocidos (no son residuos)

- **Auditoría:** cada retiro HU051 ejecutado por el E2E dejó su fila en `AuditoriaEvento` (`RETIRO_ESTUDIANTE_GRUPO`, 19 filas en total al terminar, incluidas 4 de la validación E2E de NOTIF-001 del 29/10). Se conservan a propósito: el rastro de auditoría no se edita desde scripts de prueba.
- **Estado restaurado:** `NotificacionSaliente`=0, plantillas = las 4 semilla v1 activas, matrícula del estudiante de prueba en `A`.
- **Estado de los servicios:** puertos 8090 y 3025 libres, sin procesos del E2E y carpeta temporal de correos eliminada. El backend del `:8080` y el frontend del `:4300` son de otras personas/sesiones y no se tocaron.

## Hallazgos y correcciones durante la implementación

| Hallazgo | Corrección |
|---|---|
| `PlantillaRenderer#envolverEnLayout` sustituía `{{pie}}` dentro del contenido si un dato contenía ese literal | Sustitución en una sola pasada (`fda7ab9`); CONTRACT §10.2 |
| `platform-url` sin validar esquema | Validación `http://`/`https://` fail-fast (`3cb8d71`); CONTRACT §10.2 |
| `esCorreoSeguro` dejaba pasar `, ; < > "` | Endurecido (`93603e0` RED, `1b7f5ba` GREEN); CONTRACT §10.5 |
| Los repositorios con varios result sets cerraban la sesión en el hilo del despachador | `EntityManager` dedicado fuera de transacción (`5d62bea`) |
| `NotificacionOutboxDispatcherTest`: 15 pruebas fallaban con el mock del SPI | Una línea en el `setUp` autorizada por el owner (CONTRACT §10.4) |

## Pendientes y límites

- Datos del SMTP institucional (host, puerto, TLS, remitente) y SPF/DKIM/DMARC: los entrega TI; no bloquean este work item.
- **NOTIF-002B** (administración de plantillas por el Administrador) queda fuera de alcance.
- El modo de pruebas con redirección es una medida de pruebas: debe estar apagado en producción (`SMTP_NOTIFICATION_TEST_MODE_ENABLED=false`, valor por defecto).
- Esta rama depende de NOTIF-001, que sigue sin publicar: los PR de NOTIF-001 (DB → Backend) van primero y los de NOTIF-002A después, sin auto-merge.
