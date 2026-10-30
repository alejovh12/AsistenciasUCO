---
status: frozen-approved
type: test-plan
scope: database-backend
last-reviewed: 2026-10-29
---

# TEST_PLAN — NOTIF-002A

Regla general: **RED real antes de GREEN** (pruebas que fallan por la razón correcta, no `fail()` de relleno). Ninguna prueba existente se relaja ni se borra; solo se modifica ante un error técnico genuino o un cambio formal de contrato (`TEST_CONTRACT_CONFLICT`: se detiene y se consulta).

Metas de cobertura del código nuevo: ≥ 90 % líneas y ≥ 85 % ramas (reportar instrucciones y líneas por separado). Gate mínimo global de JaCoCo sin cambios.

## 1. Base de datos (T-SQL transaccional, `GestioAsistenciaDB/test/`)

| ID | Caso |
| :-- | :-- |
| DB-P01 | `usp_consultar_plantilla_notificacion_activa` devuelve la plantilla v1 sembrada de cada uno de los 4 tipos (`asunto`, `cuerpo`, `version`) y sus marcadores con `obligatorio` correcto |
| DB-P02 | Tipo sin plantilla → éxito y 0 filas (no error) |
| DB-P03 | `@tipo` con formato inválido → `NTF_001`; `@idCorrelacion` nulo → `CORR_001` |
| DB-P04 | Con dos versiones, solo la `activa=1` se devuelve; el índice único filtrado impide dos activas para el mismo `(tipo, idioma)` |
| DB-P05 | Idioma inexistente → 0 filas |
| DB-C01 | `usp_consultar_contacto_notificacion` devuelve `correo` y `correoConfirmado` de usuarios existentes y habilitados |
| DB-C02 | Usuario inhabilitado (`estado=0`) o inexistente no aparece |
| DB-C03 | `@usuariosJson` vacío, no JSON o con más de 100 elementos → `NTF_001`; `@idCorrelacion` nulo → `CORR_001` |
| DB-C04 | Elementos que no son UUID válidos → `NTF_001` (sin excepción cruda) |
| DB-M01 | Semilla idempotente: ejecutar la migración dos veces no duplica plantillas ni marcadores |
| DB-M02 | Migración `down` retira solo objetos de NOTIF-002A |
| DB-A01 | Aislamiento: ningún objeto del resto del modelo referencia `PlantillaNotificacion*` (sin FK entrantes) |
| DB-G01 | `rol_asistencias_runtime` puede ejecutar los 2 SP y no puede hacer DML directo sobre las tablas nuevas |
| DB-G02 | `@@TRANCOUNT = 0` al terminar; sin transacciones huérfanas |

Registro en `test_suite.sql` y excepciones en `test_contract_closure.sql` solo si hacen falta (**archivos calientes: commit aparte; si se modifica un test existente, declararlo**).

## 2. Backend — unitarias

### 2.1 Renderizador (`PlantillaRendererTest`)

| ID | Caso |
| :-- | :-- |
| BE-R01 | Sustituye `{grupo}` y `{estudiante}` con datos válidos; `origen=db` |
| BE-R02 | Valor con `<script>alert(1)</script>`, `&`, `"` y `'` → escapado en HTML y literal en texto plano |
| BE-R03 | Falta un marcador obligatorio en `datos` → baja a plantilla por defecto (`origen=default`) |
| BE-R04 | Falta en el nivel por defecto también → usa `asunto`/`mensaje` guardados (`origen=stored`) |
| BE-R05 | Marcador no declarado para el tipo (`{inventado}`) → no se sustituye, el nivel se descarta y se baja |
| BE-R06 | Párrafos (línea en blanco) → `<p>`; salto simple → `<br>`; el HTML de la plantilla no se interpreta |
| BE-R07 | Asunto con saltos de línea → saltos reemplazados por espacio; recorte a 200 |
| BE-R08 | `{` sin cierre → queda literal escapado, sin excepción |
| BE-R09 | Con `platform-url` el pie incluye el enlace; sin él, no hay enlace |
| BE-R10 | La alternativa de texto plano nunca contiene etiquetas HTML ni valores escapados |
| BE-R11 | `motivo` presente en `datos` pero no usado por la plantilla semilla → no aparece en el cuerpo |
| BE-R12 | Determinismo: misma entrada → misma salida |

### 2.2 Repositorios de plantilla y contacto

| ID | Caso |
| :-- | :-- |
| BE-T01 | Mapeo posicional de `usp_consultar_plantilla_notificacion_activa` (3 result sets); 0 filas → `Optional.empty()` |
| BE-T02 | Caché por `(tipo, idioma)` con TTL: segunda lectura no vuelve a la BD; vencido el TTL, sí (reloj inyectable) |
| BE-T03 | Falla de BD al leer plantilla → no propaga: el renderizador baja al respaldo y cuenta `origin` correspondiente |
| BE-C01 | Mapeo de contactos (`idUsuario`, `correo`, `correoConfirmado`) con tipos de UUID heterogéneos |
| BE-C02 | Error del SP (`exitoso=0`) → excepción transitoria; JSON de entrada con los `idUsuario` correctos |

### 2.3 Sender SMTP (`SmtpNotificacionChannelSenderTest`, con `CorreoTransport` falso)

| ID | Caso |
| :-- | :-- |
| BE-S01 | Notificación con 3 destinatarios válidos → 1 mensaje, `To`=remitente, 3 BCC, multipart texto+HTML, `Message-ID` derivado del id |
| BE-S02 | Destinatario sin correo o inhabilitado → omitido; resto enviado; métrica `omitted` |
| BE-S03 | Ningún destinatario válido → `NotificacionNoEntregableException` |
| BE-S04 | `require-confirmed-email=true` omite no confirmados; con `false` los incluye |
| BE-S05 | Más de `max-bcc` destinatarios → bloques; todos enviados |
| BE-S06 | Fallo transitorio (conexión/timeout/4xx) → excepción no controlada (el despachador reintenta) |
| BE-S07 | Fallo permanente (dirección rechazada/5xx) → `NotificacionNoEntregableException` |
| BE-S08 | Inyección de cabeceras: asunto o dirección con `\r\n` → saneados/rechazados; nunca llegan al transporte |
| BE-S09 | El sender nunca loguea correos, asunto ni cuerpo (captura de logs) |
| BE-S10 | Plantilla en BD no disponible → se envía con el respaldo y no se pierde el correo |

### 2.4 Tope de envíos (`TopeEnviosTest`, reloj inyectable)

| ID | Caso |
| :-- | :-- |
| BE-K01 | Hasta `rate-per-minute` envíos por minuto; el siguiente → `disponible()=false` hasta la ventana siguiente |
| BE-K02 | Tope diario independiente del minuto |
| BE-K03 | Concurrencia: N hilos no superan el tope |
| BE-K04 | `disponible()=false` en el sender → el despachador no reserva filas ni incrementa `intentos` (`NotificacionOutboxDispatcherTest` ampliado, sin tocar aserciones previas) |
| BE-K05 | Sender `log` (`disponible()` por defecto) → comportamiento idéntico a NOTIF-001 |

### 2.5 Propiedades (`SmtpNotificationPropertiesTest`)

| ID | Caso |
| :-- | :-- |
| BE-P01 | Defaults del CONTRACT §3.1 |
| BE-P02 | Rechaza: host vacío, puerto fuera de rango, `from-address` inválido, `starttls`+`ssl` a la vez, `rate-per-day < rate-per-minute`, `max-bcc` fuera de 1..100 |
| BE-P03 | La contraseña no aparece en `toString()` |

### 2.6 Casos de uso emisores (claves nuevas en `datos`)

| ID | Caso |
| :-- | :-- |
| BE-U01 | Cada uno de los 4 casos de uso incluye en `datos` las claves del CONTRACT §4.4 con valores legibles |
| BE-U02 | `tipo`, `asunto`, `mensaje` y las claves previas de `datos` permanecen **idénticos** a los de antes (aserción nueva sobre las claves previas; las pruebas existentes no se modifican) |

### 2.7 Modo de pruebas con redirección (D15)

| ID | Caso |
| :-- | :-- |
| BE-P05 | Defaults: `test-mode.enabled=false` y `redirect-to` vacío |
| BE-P06 | `enabled=true` con `redirect-to` vacío, con una dirección insegura (`a@b.co,c.com`, `<a@b.co>`, con espacios o CR/LF) o con más de 5 direcciones → `IllegalArgumentException` (fail-fast) |
| BE-P07 | `redirect-to` con `"a@x.co, b@x.co ,a@x.co"` → 2 direcciones recortadas y sin duplicados; con `enabled=false` el valor se ignora sin error |
| BE-R14 | Sobrecarga del renderizador con aviso: el aviso aparece como primer párrafo en texto plano y en HTML (escapado); sin aviso, la salida es idéntica a la de la firma de 4 argumentos |
| BE-S11 | Modo activo: el transporte recibe como BCC solo `redirect-to`; ninguna dirección original aparece en BCC, `To`, asunto, texto ni HTML |
| BE-S12 | Modo activo: el asunto empieza por `[PRUEBA] ` y no supera 200 caracteres ni contiene saltos de línea |
| BE-S13 | Modo activo: el aviso indica el número `N` de destinatarios originales omitidos y no contiene direcciones ni nombres |
| BE-S14 | Modo activo: cada mensaje incrementa `notification.email.redirected.total`; con el modo apagado el contador no se incrementa |
| BE-S15 | Modo apagado: sin prefijo ni aviso; comportamiento idéntico al de BE-S01 |
| BE-S16 | Modo activo sin destinatarios válidos → `NotificacionNoEntregableException` y **nada** se envía a `redirect-to` |
| BE-S17 | Modo activo con más de `max-bcc` direcciones de `redirect-to` → bloques; todas las direcciones de pruebas reciben el correo |

## 3. Wiring y arquitectura

| ID | Caso |
| :-- | :-- |
| WI-S01 | `sender=log` (default): no se crean beans `smtp`; `NotificacionChannelSender` es el sender `log` |
| WI-S02 | `sender=smtp` con propiedades válidas: se crean `SmtpNotificacionChannelSender`, `TopeEnvios`, renderizador, repositorios |
| WI-S03 | `sender=smtp` con `host` o `from-address` vacíos → el contexto no arranca (fail-fast) |
| WI-S04 | `sender=desconocido` → no arranca |
| WI-S06 | `test-mode.enabled=true` sin `redirect-to` válido → el contexto no arranca; con `redirect-to` válido arranca y registra el `WARN` de modo de pruebas sin las direcciones |
| WI-S05 | El fixture **no** declara beans que el runtime real no provee (lección de NOTIF-001: `ObjectMapper`): cada bean externo usado debe existir en el arranque real |
| AR-07 | Solo `config` instancia clases concretas de `notification.template/contact/channel.smtp` |
| AR-08 | `application..` y controladores no dependen de `notification.template`, `contact` ni `channel.smtp` |
| AR-09 | `notification.template` y `notification.contact` no dependen de `channel.smtp` (dependencia en un solo sentido) |
| AR-10 | Sin `@Component`/`@Service` en clases del módulo; sin `JdbcTemplate` ni SQL inline fuera de los repositorios |

## 4. Integración (failsafe `-Pintegration`, entorno cargado)

Antes de escribir las IT, guardar el **baseline** de `mvnw -Pintegration failsafe:integration-test failsafe:verify` sobre `origin/develop` con el mismo entorno (el `.env` puede empezar con BOM: cargarlo sin BOM; ClamAV/MinIO/datasource definidos). La única diferencia admisible entre baseline y rama son las IT nuevas en verde.

| ID | Caso (GreenMail + SQL Server real) |
| :-- | :-- |
| IT-E01 | `RETIRO_ESTUDIANTE_GRUPO` registrado en el outbox → despachado → GreenMail recibe 1 correo con asunto y cuerpo de la plantilla en BD |
| IT-E02 | Multipart: parte `text/plain` y parte `text/html`; HTML con el layout fijo y valores escapados |
| IT-E03 | Cambiar la plantilla activa (nueva versión activa) → el siguiente correo usa el contenido nuevo (tras el TTL de caché) |
| IT-E04 | Sin plantilla activa → se envía con el respaldo por defecto |
| IT-E05 | Usuario destinatario inhabilitado → omitido; el resto recibe el correo |
| IT-E06 | Ningún destinatario con correo → fila `FALLIDA_DEFINITIVA` sin reintentos |
| IT-E07 | SMTP caído → reintentos con backoff → recupera al volver el servidor; fila `ENTREGADA` |
| IT-E08 | Tope de envíos agotado → filas quedan `PENDIENTE` sin consumir intentos; se despachan al liberarse cupo |
| IT-E09 | Los 4 tipos producen correo legible y distinguible |
| IT-E11 | Modo de pruebas (GreenMail): solo los buzones de `redirect-to` reciben el correo; los buzones de los destinatarios reales quedan vacíos; el asunto lleva `[PRUEBA]` y el cuerpo el aviso con `N` correcto |
| IT-E10 | Ningún log del contexto contiene correos ni cuerpo (captura de logs) |

## 5. Compatibilidad

`NotificacionOutboxIT` (NOTIF-001), `NotificationWiringTest`, `LogNotificacionAdapterTest` y las pruebas de los 4 casos de uso permanecen sin cambios y en verde.

## 6. E2E (puerto 8090, `APP_ADAPTERS_SECURITY_PROVIDER=legacy`; declarar que no se probó contra el PDP)

Con un **sumidero SMTP local sin Docker** (scratch, no versionado: una clase Java que levanta GreenMail —ya presente en el `pom.xml` como dependencia de test; classpath con `mvnw dependency:build-classpath -Dmdep.includeScope=test`— en `127.0.0.1:3025` y vuelca cada mensaje recibido como `.eml` en un directorio temporal **fuera del repositorio**), `sender=smtp`, `host=127.0.0.1`, `port=3025`, y el endpoint HU051 `POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro` con JWT reales.

| ID | Caso |
| :-- | :-- |
| E2E-M01 | Arranque con `sender=smtp` sin errores; con `sender=log` sin beans SMTP |
| E2E-M02 | El retiro HU051 produce un correo en el sumidero con asunto/cuerpo de la plantilla; BCC con los destinatarios esperados |
| E2E-M03 | El cuerpo **no** contiene el motivo del retiro (D12) |
| E2E-M04 | HTML escapado y alternativa de texto plano presentes (verificado leyendo los `.eml` del sumidero) |
| E2E-M05 | Cambiar la plantilla en BD (script de prueba, versión nueva activa) y verificar el siguiente correo; **restaurar** la semilla al terminar |
| E2E-M06 | Sumidero detenido → el retiro responde 2xx, la fila queda reintentable y se entrega al volver el sumidero |
| E2E-M07 | Métricas `notification.email.send.total` y `notification.template.render.total` presentes |
| E2E-M09 | Modo de pruebas (`SMTP_NOTIFICATION_TEST_MODE_ENABLED=true`, `SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO` con 2 direcciones ficticias): el retiro HU051 produce un único correo cuyo BCC es solo `redirect-to`, con asunto `[PRUEBA]` y aviso; **ninguna** dirección real de los destinatarios aparece en ningún `.eml`; arranque con `WARN` de modo de pruebas sin direcciones; métrica `notification.email.redirected.total` presente |
| E2E-M08 | Cero residuos: filas de prueba en `NotificacionSaliente` eliminadas, matrícula restaurada, plantilla restaurada, sumidero detenido y su directorio temporal eliminado, puerto 8090 libre |

Cualquier caso que exija alterar la BD compartida de forma no restaurable se declara `NO_EJECUTADO` con su razón.

## 7. Matriz de trazabilidad

| Requisito | Pruebas |
| :-- | :-- |
| D3 orden de resolución | BE-R01..R05, BE-S10, IT-E03, IT-E04 |
| D4/D5/D6 estructura fija, marcadores, escape | BE-R02, BE-R05..R10, IT-E02 |
| D7 contacto al entregar | DB-C*, BE-C*, BE-S02..S04, IT-E05, IT-E06 |
| D8/D9 BCC y errores | BE-S01, BE-S05..S07, IT-E07 |
| D10 tope | BE-K*, IT-E08 |
| D11 selector y fail-fast | BE-P*, WI-S* |
| D12/D13 privacidad | BE-R11, BE-S09, IT-E10, E2E-M03 |
| §6 removibilidad | AR-07..AR-10, DB-A01 |
