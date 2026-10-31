---
status: frozen-approved
type: contract
scope: database-backend
last-reviewed: 2026-10-29
---

# CONTRACT — NOTIF-002A

Este contrato fija nombres, firmas, propiedades, códigos y métricas. Todo lo no dicho aquí sigue lo establecido en el CONTRACT de NOTIF-001 y en `docs/architecture/adapter-composition-standard.md`.

## 1. Reglas comunes

- `NotificacionPort` y el record `Notificacion` **no cambian de firma**. Los 4 casos de uso emisores solo **agregan claves** a `datos` (ver §4.4); `asunto` y `mensaje` no se modifican.
- `application` no importa nada de `infrastructure` y no contiene `CorrelationIdContext`, `MDC`, `idCorrelacion`, `IdCorrelacion`.
- Los logs **nunca** incluyen correos, `asunto`, `mensaje`, cuerpo renderizado ni nombres de persona.
- Sin GUIDs ni textos de estado quemados en T-SQL. Sin SQL inline fuera de adaptadores secundarios.
- Todo el módulo vive bajo `infrastructure.adapter.secondary.notification.*` y su configuración bajo `infrastructure.config.adapters.notification.*`. Ver §6 (removibilidad).

## 2. Base de datos (repositorio `GestioAsistenciaDB`)

### 2.1 Esquema

**`dbo.PlantillaNotificacion`** (contenido editable y versionado)

| Columna | Tipo | Notas |
| :-- | :-- | :-- |
| `id` | `UNIQUEIDENTIFIER` PK | |
| `tipo` | `VARCHAR(60)` NOT NULL | mismo dominio que `NotificacionSaliente.tipo` (`^[A-Z0-9_]+$`) |
| `idioma` | `CHAR(2)` NOT NULL | default `'es'` |
| `version` | `INT` NOT NULL | ≥ 1; `UNIQUE (tipo, idioma, version)` |
| `asunto` | `NVARCHAR(200)` NOT NULL | texto plano con marcadores `{nombre}` |
| `cuerpo` | `NVARCHAR(4000)` NOT NULL | texto plano con marcadores; párrafos separados por línea en blanco |
| `activa` | `BIT` NOT NULL | índice único filtrado: a lo sumo **una** activa por `(tipo, idioma)` |
| `creadoPor` | `UNIQUEIDENTIFIER` NULL | nulo para semillas del sistema |
| `creadoEn` | `DATETIME2(3)` NOT NULL | UTC |

**`dbo.PlantillaNotificacionMarcador`** (catálogo de marcadores válidos por tipo)

| Columna | Tipo | Notas |
| :-- | :-- | :-- |
| `id` | `UNIQUEIDENTIFIER` PK | |
| `tipo` | `VARCHAR(60)` NOT NULL | |
| `marcador` | `VARCHAR(40)` NOT NULL | sin llaves; `^[a-zA-Z][a-zA-Z0-9]*$`; `UNIQUE (tipo, marcador)` |
| `descripcion` | `NVARCHAR(200)` NOT NULL | ayuda para el administrador (002B) |
| `ejemplo` | `NVARCHAR(100)` NOT NULL | valor de muestra para la vista previa (002B) |
| `obligatorio` | `BIT` NOT NULL | si falta en `datos`, se baja al respaldo |

Migración idempotente `up/down` (`08_notif002a`), semilla idempotente por clave natural (`tipo`+`idioma`+`version` y `tipo`+`marcador`), sin GUID quemado en lógica (los ids de semilla se generan con `NEWID()` condicionado a inexistencia o se leen por clave natural).

### 2.2 Semillas (marcadores y plantillas v1, `idioma='es'`, `activa=1`, `creadoPor=NULL`)

| Tipo | Marcadores (O = obligatorio) | Asunto semilla | Cuerpo semilla |
| :-- | :-- | :-- | :-- |
| `RETIRO_ESTUDIANTE_GRUPO` | `grupo` (O), `estudiante` (O), `motivo` | `Retiro del grupo {grupo}` | `Se registró el retiro de {estudiante} del grupo {grupo}.` |
| `SOLICITUD_INGRESO_REGISTRADA` | `grupo` (O), `estudiante` (O) | `Nueva solicitud de ingreso al grupo {grupo}` | `{estudiante} solicitó ingresar al grupo {grupo}. Revísala en la plataforma.` |
| `SOLICITUD_INGRESO_RESUELTA` | `grupo` (O), `resultado` (O) | `Respuesta a tu solicitud de ingreso al grupo {grupo}` | `Tu solicitud de ingreso al grupo {grupo} fue {resultado}. Consulta el detalle en la plataforma.` |
| `SOLICITUD_INGRESO_RETIRADA` | `grupo` (O), `estudiante` (O) | `Solicitud de ingreso retirada del grupo {grupo}` | `{estudiante} retiró su solicitud de ingreso al grupo {grupo}.` |

El marcador `motivo` existe y es válido para `RETIRO_ESTUDIANTE_GRUPO`, pero **la plantilla semilla no lo usa** (D12, Q1). El texto exacto de las semillas puede ajustarse en la aprobación del owner; la estructura es fija.

### 2.3 Procedimientos (técnicos, ejecutados solo por el runtime; excepción RBAC documentada igual que NOTIF-001 D9)

Fachada pública `usp_*` + interno `usp_internal_*` donde haya lógica; `SET XACT_ABORT ON`; resultado uniforme `idResultado, exitoso, mensajeUsuario, mensajeTecnico` como primer result set y los datos como segundo.

#### `dbo.usp_consultar_plantilla_notificacion_activa`

| Parámetro | Tipo | Notas |
| :-- | :-- | :-- |
| `@tipo` | `NVARCHAR(100)` | `UPPER(TRIM)`; formato inválido → `NTF_001` |
| `@idioma` | `CHAR(2)` | default `'es'` |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | nulo → `CORR_001` |

Devuelve **0 o 1 fila**: `version`, `asunto`, `cuerpo`, más la lista de marcadores del tipo (`marcador`, `obligatorio`) en un tercer result set. Sin plantilla activa devuelve éxito con 0 filas (no es error: el backend usa respaldo). Éxito `GEN_003`.

#### `dbo.usp_consultar_contacto_notificacion`

| Parámetro | Tipo | Notas |
| :-- | :-- | :-- |
| `@usuariosJson` | `NVARCHAR(MAX)` | arreglo JSON de 1..100 UUID; vacío o inválido → `NTF_001` |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | nulo → `CORR_001` |

Devuelve una fila por usuario **existente y habilitado** (`Usuario.estado = 1`): `idUsuario`, `correo`, `correoConfirmado`. Usuarios inexistentes o inhabilitados no aparecen (el backend los omite). Es una consulta técnica por lote acotado (≤ 100) y no una consulta paginada de listados de UI; se documenta como excepción a la regla de paginación (no expone un listado). Éxito `GEN_003`.

> El implementador **verifica cómo se otorgan hoy los permisos de ejecución** a los SP técnicos de NOTIF-001 (`rol_asistencias_runtime_notif001.sql`) y replica el mecanismo en un archivo `rol_asistencias_runtime_notif002a.sql`. Si hiciera falta ampliar privilegios más allá de `GRANT EXECUTE` sobre estos dos SP, **se detiene y consulta**.

### 2.4 Códigos de mensaje

Se reutilizan `NTF_001` (datos inválidos), `CORR_001` y `GEN_003`. No se agregan códigos en 002A (los nuevos para edición de plantillas pertenecen a 002B).

## 3. Backend — selección y propiedades

### 3.1 `application.yml` (archivo caliente: commit aparte, al final)

```yaml
app:
  providers:
    outbox-notification:
      sender: ${OUTBOX_NOTIFICATION_SENDER:log}                    # log | smtp
    smtp-notification:
      host: ${SMTP_NOTIFICATION_HOST:}
      port: ${SMTP_NOTIFICATION_PORT:587}
      username: ${SMTP_NOTIFICATION_USERNAME:}
      password: ${SMTP_NOTIFICATION_PASSWORD:}                     # nunca en el repositorio
      starttls: ${SMTP_NOTIFICATION_STARTTLS:true}
      ssl: ${SMTP_NOTIFICATION_SSL:false}
      from-address: ${SMTP_NOTIFICATION_FROM_ADDRESS:}
      from-name: ${SMTP_NOTIFICATION_FROM_NAME:Asistencia UCO}
      connect-timeout: ${SMTP_NOTIFICATION_CONNECT_TIMEOUT:10s}
      read-timeout: ${SMTP_NOTIFICATION_READ_TIMEOUT:20s}
      max-bcc: ${SMTP_NOTIFICATION_MAX_BCC:50}                     # 1..100
      rate-per-minute: ${SMTP_NOTIFICATION_RATE_PER_MINUTE:60}     # 1..10000
      rate-per-day: ${SMTP_NOTIFICATION_RATE_PER_DAY:500}          # >= rate-per-minute
      require-confirmed-email: ${SMTP_NOTIFICATION_REQUIRE_CONFIRMED_EMAIL:false}
      platform-url: ${SMTP_NOTIFICATION_PLATFORM_URL:}             # vacío => el correo no incluye enlace (si se define: http:// o https://)
      test-mode:
        enabled: ${SMTP_NOTIFICATION_TEST_MODE_ENABLED:false}      # D15: true => redirige TODO a redirect-to
        redirect-to: ${SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO:}   # 1..5 direcciones separadas por coma; obligatorio si enabled=true
```

Reglas de validación (fail-fast al arrancar, con `sender=smtp`): `host` no vacío; `port` 1..65535; `from-address` con formato de correo; `starttls` y `ssl` no simultáneos; `rate-per-day >= rate-per-minute`; con `test-mode.enabled=true`, `redirect-to` debe contener entre 1 y 5 direcciones válidas (mismo criterio de seguridad que `esCorreoSeguro`: sin espacios, saltos de línea ni `, ; < > "` dentro de cada dirección). Con `sender=log` ninguna propiedad `smtp-notification.*` es obligatoria y no se crean beans SMTP.

La clave `password` se resuelve **sin imprimirla**: por variable de entorno o por `SecretVaultPort` (el implementador sigue el patrón ya usado para secretos y lo documenta; no se inventa uno nuevo).

### 3.2 Properties tipadas

`SmtpNotificationProperties` (`@ConfigurationProperties("app.providers.smtp-notification")`, validación en el constructor del record, igual que `OutboxNotificationProperties`).

## 4. Backend — componentes

### 4.1 Paquetes (todos bajo `infrastructure.adapter.secondary.notification`)

| Paquete | Contenido |
| :-- | :-- |
| `outbox` (existente) | `NotificacionChannelSender` (agrega `default boolean disponible() { return true; }`), `NotificacionOutboxDispatcher` (consulta `disponible()` antes de reservar), `LogNotificacionChannelSender` |
| `template` | `PlantillaNotificacion` (record: `version`, `asunto`, `cuerpo`, `marcadores`), `PlantillaNotificacionRepository` (SPI), `PlantillaNotificacionJpaRepository` (adaptador JPA/`JpaProcedureExecutor` sobre `usp_consultar_plantilla_notificacion_activa`), `PlantillaPorDefectoCatalogo` (recurso `notification/templates/<TIPO>.es.properties`), `PlantillaRenderer`, `ContenidoRenderizado` (record: `asunto`, `textoPlano`, `html`, `origen`) |
| `contact` | `ContactoDestinatario` (record: `idUsuario`, `correo`, `correoConfirmado`), `ContactoDestinatarioResolver` (SPI), `ContactoDestinatarioJpaRepository` (sobre `usp_consultar_contacto_notificacion`) |
| `channel.smtp` | `CorreoTransport` (SPI: `enviar(CorreoSaliente)`), `JakartaMailCorreoTransport`, `CorreoSaliente` (record), `TopeEnvios` (token bucket por minuto y por día, reloj inyectable), `SmtpNotificacionChannelSender` |

Recursos: `notification/email/layout.html` (layout fijo con `{{contenido}}`, `{{pie}}`) y `notification/templates/*.properties` (respaldo por tipo).

### 4.2 Renderizador (`PlantillaRenderer`)

Entrada: `tipo`, `datos`, `asuntoGuardado`, `mensajeGuardado`. Salida: `ContenidoRenderizado`.

1. Plantilla activa en BD → si falta, 2. plantilla por defecto del backend → si falta o no se puede renderizar, 3. `asuntoGuardado`/`mensajeGuardado`.
2. Un nivel solo se usa si **todos** sus marcadores obligatorios existen y no vacíos en `datos` y si todo `{marcador}` presente en el texto está declarado para el tipo. Si no, se baja de nivel y se cuenta `origin=fallback`.
3. Sustitución: `{nombre}` → valor de `datos["nombre"]` **escapado para HTML** en la versión `html` y sin escapar en `textoPlano`. Un `{` sin cierre o sin marcador válido se deja literal escapado.
4. Conversión del contenido a HTML: líneas en blanco → `<p>`; saltos simples → `<br>`; **ningún** HTML ni enlace automático del texto de la plantilla. Si `platform-url` está definido, el pie agrega un enlace a esa URL (valor de configuración, no de datos).
5. `asunto`: sin saltos de línea (se reemplazan por espacio), recortado a 200 caracteres.
6. Determinista y sin E/S (la plantilla se recibe resuelta); el repositorio se consulta antes y se cachea por `(tipo, idioma)` durante `template-cache-ttl` (default 60 s) para no consultar la BD por cada envío.

### 4.3 `SmtpNotificacionChannelSender`

`enviar(NotificacionPersistida n)`:

1. Resolver contactos de `n.destinatarios()` (D7). Omitir sin correo/inhabilitados y, si `require-confirmed-email=true`, los no confirmados. Sin destinatarios → `NotificacionNoEntregableException`.
2. Renderizar (§4.2).
3. Armar `CorreoSaliente` (remitente configurado, `To` = remitente, BCC en bloques de `max-bcc`, multipart `text/plain` + `text/html`, `Message-ID` derivado de `idNotificacion` para trazabilidad).
4. Consumir cupo de `TopeEnvios`; si no hay cupo se lanza una excepción transitoria (no debería ocurrir porque el despachador consulta `disponible()` antes).
5. Enviar por `CorreoTransport`; clasificar el fallo (D9): dirección rechazada o respuesta 5xx → `NotificacionNoEntregableException`; conexión, timeout o 4xx → excepción transitoria.

`disponible()` devuelve `false` mientras el tope del minuto o del día esté agotado.

### 4.4 Claves nuevas en `Notificacion.datos` (solo agregar; nunca quitar ni cambiar las existentes)

| Tipo | Claves agregadas |
| :-- | :-- |
| `RETIRO_ESTUDIANTE_GRUPO` | `grupo` (= `nombreGrupo de nombreAsignatura`), `estudiante` |
| `SOLICITUD_INGRESO_REGISTRADA` | `grupo`, `estudiante` |
| `SOLICITUD_INGRESO_RESUELTA` | `grupo`, `resultado` (`aceptada` / `puesta en espera` / `rechazada`) |
| `SOLICITUD_INGRESO_RETIRADA` | `grupo`, `estudiante` |

El implementador inspecciona los 4 casos de uso para tomar los valores ya disponibles (sin consultas nuevas). Si una prueba existente compara el mapa `datos` completo y falla por las claves agregadas, es `TEST_CONTRACT_CONFLICT`: **se detiene y consulta**.

### 4.5 Composition Roots (`infrastructure.config.adapters.notification.*`)

- `OutboxNotificacionAdapterConfiguration` (existente) sigue creando el sender `log` con `sender=log` (default).
- `SmtpNotificacionAdapterConfiguration` (nueva): `@Conditional` de outbox **y** `sender=smtp`; crea `SmtpNotificationProperties`, `TopeEnvios`, `JakartaMailCorreoTransport`, repositorios de plantilla y contacto, renderizador y `SmtpNotificacionChannelSender` como `NotificacionChannelSender`. Sin `@Component`/`@Service` en las clases del módulo.
- `sender` con valor desconocido → el arranque falla (fail-closed).

### 4.6 Modo de pruebas con redirección (D15)

Propiedades: `app.providers.smtp-notification.test-mode.enabled` (default `false`) y `test-mode.redirect-to` (lista de 1..5 direcciones; el valor de entorno admite comas, se recortan espacios y se eliminan duplicados). Con `enabled=true` y `redirect-to` vacío o con una dirección insegura el arranque falla (fail-fast). Con `enabled=false` el valor de `redirect-to` se ignora y el comportamiento es **idéntico** al de producción.

Comportamiento de `SmtpNotificacionChannelSender` con el modo activo:

1. Los contactos se resuelven y filtran igual que siempre (para decidir si existe algún destinatario válido). Si no hay ninguno, sigue siendo `NotificacionNoEntregableException`: **no** se envía nada al buzón de pruebas.
2. Los destinatarios reales **nunca** llegan al transporte: el BCC se reemplaza por `redirect-to` (en bloques de `max-bcc`, `To` = remitente). Ninguna dirección original aparece en cabeceras, asunto ni cuerpo.
3. El asunto se prefija con `[PRUEBA] ` (después de sanear y antes de recortar a 200 caracteres).
4. El cuerpo (texto plano y HTML) comienza con un párrafo de aviso: `MODO DE PRUEBAS: este correo no se envió a los destinatarios reales. Destinatarios originales omitidos: {N}.`, donde `N` es el número de destinatarios válidos (sin direcciones ni nombres). El aviso lo inserta el renderizador mediante una **sobrecarga** `renderizar(tipo, datos, asuntoGuardado, mensajeGuardado, avisoModoPrueba)`; la firma de 4 argumentos existente no cambia.
5. Métrica nueva `notification.email.redirected.total` (counter, sin etiquetas) por cada mensaje redirigido. Al arrancar con el modo activo se registra un `WARN` único ("modo de pruebas de correo ACTIVO: los correos se redirigen") **sin** las direcciones.
6. El tope de envíos, la clasificación de errores y los reintentos funcionan igual.

Es una medida de pruebas, no de seguridad: debe estar apagada en producción (el default es `false` y el `WARN` de arranque lo hace visible).

### 4.7 Observabilidad (Micrometer; nombres fijados)

| Métrica | Tipo | Etiquetas |
| :-- | :-- | :-- |
| `notification.email.send.total` | counter | `outcome` (`sent`/`retry`/`permanent`/`no_recipients`) |
| `notification.email.recipients.total` | counter | `outcome` (`sent`/`omitted`) |
| `notification.email.throttled.total` | counter | `window` (`minute`/`day`) |
| `notification.email.redirected.total` | counter | — (solo con `test-mode.enabled=true`) |
| `notification.template.render.total` | counter | `origin` (`db`/`default`/`stored`) |
| `notification.email.send.duration` | timer | `outcome` |

Etiquetas de baja cardinalidad (nunca `tipo` libre ni correos). Logs con `idNotificacion`, `tipo`, `destinatarios` (conteo), `origen`, `outcome`.

## 5. Compatibilidad

- `LogNotificacionChannelSender`, los tres proveedores `log`/`outbox`/`composite` y NOTIF-001 completo se conservan y sus pruebas siguen verdes. Cambio compatible en el SPI: `disponible()` con implementación por defecto.
- Con `sender=log` (default) el comportamiento es idéntico al de NOTIF-001.
- Sin cambios en `SecurityConfig`, `access-catalog.json`, OpenAPI ni contratos WORM.

## 6. Removibilidad (reglas verificables)

1. **Solo `config` instancia clases concretas** del módulo; ninguna clase fuera de `notification.*` y de `config.adapters.notification.*` las referencia (ArchUnit AR-07).
2. Ningún `application.*` ni controlador importa nada de `notification.template`, `notification.contact` o `notification.channel.smtp` (AR-08).
3. Las tablas `PlantillaNotificacion*` no tienen claves foráneas hacia el resto del modelo y nada las referencia; sus SP llevan el prefijo `usp_*plantilla_notificacion*` / `usp_*contacto_notificacion` (prueba T-SQL de aislamiento).
4. **Sustitución del canal** (componente de otro equipo): basta una nueva implementación de `NotificacionChannelSender` (p. ej. `HttpNotificacionChannelSender`) y `sender=<nuevo>`; el renderizador puede reutilizarse o no.
5. **Sustitución completa**: un nuevo valor de `app.adapters.notification.provider` que implemente `NotificacionPort`; las tablas de outbox y plantillas quedan sin uso y se retiran con una migración de limpieza.
6. **Exportación**: script de volcado de plantillas activas (seguimiento §7.5 del PLAN).

## 7. Seguridad y datos

- Credenciales SMTP fuera del repositorio y de los logs; `.env.template` documenta solo nombres de variable.
- Escape de HTML obligatorio (D6); pruebas con valores hostiles (`<script>`, comillas, `&`).
- Cabecera de correo sin inyección: asunto y direcciones sin saltos de línea (prueba de inyección de cabeceras).
- Minimización: el correo no se persiste; el contenido renderizado no se guarda ni se loguea.
- Tope de envíos protege la cuenta remitente y al destinatario.

## 8. Documentación a actualizar (al final, commit aparte por archivo)

`docs/architecture/adapter-composition-standard.md` (sender `smtp`, módulo notification), `docs/adr/ADR-006-notification-email-and-templates.md` (nuevo), `docs/baseline/TECHNICAL_DEBT.md` (seguimientos del PLAN §7; actualizar TD-067 y TD-068), `.env.template` (nombres de variables), guía breve de uso del modo de pruebas con redirección (D15).

## 9. Fuera de alcance

Endpoints y pantalla de administración (NOTIF-002B), otros canales, preferencias por usuario, adjuntos, bandeja web, proveedor externo.

## 10. Enmiendas

### 10.1 Nota operativa sobre semillas v1 para NOTIF-002B
- **Fecha:** 2026-10-29
- **Ámbito:** Base de datos — `schema/seed/26_plantillas_notificacion.sql` y `migrations/08_notif002a_plantillas_notificacion_up.sql`.
- **Nota:** El `MERGE` idempotente de semillas actualiza `asunto` y `cuerpo` de `version = 1` cuando coincide `(tipo, idioma, version = 1)` sin tocar la columna `activa` en `WHEN MATCHED`. En `NOTIF-002B` (edición de plantillas por el Administrador), toda versión editada por el Administrador se creará con `version >= 2`, y la semilla del sistema nunca debe reactivar `version = 1` ni alterar versiones `>= 2`.

### 10.2 Precisión de sustitución de layout en `PlantillaRenderer` y validación de `platform-url` en `SmtpNotificationProperties`
- **Fecha:** 2026-10-30
- **Ámbito:** Backend — `PlantillaRenderer#envolverEnLayout` y `SmtpNotificationProperties`.
- **Precisión (no desvío):**
  1. `PlantillaRenderer#envolverEnLayout` sustituye primero `{{pie}}` y luego `{{contenido}}` (evitando que un valor de `datos` que contenga el literal `{{pie}}` o `{{contenido}}` sea reemplazado por el pie del correo dentro del contenido renderizado).
  2. `SmtpNotificationProperties` valida fail-fast que, si `platformUrl` no está vacía tras recortar espacios, empiece obligatoriamente por `http://` o `https://` (rechazando con `IllegalArgumentException` esquemas como `javascript:`, `ftp:` o texto libre).

### 10.3 Resolución de `TEST_CONTRACT_CONFLICT` en `NotificationWiringTest#WI08_sender_desconocido_falla`
- **Fecha:** 2026-10-30
- **Ámbito:** Backend — `NotificationWiringTest.java` línea 168.
- **Motivo y decisión del owner:** En `NOTIF-001`, el caso `WI08_sender_desconocido_falla` usaba `"app.providers.outbox-notification.sender=smtp"` como ejemplo de valor desconocido porque `OutboxNotificationProperties.Sender` solo tenía `LOG`. En `NOTIF-002A` (`CONTRACT §3.1` y `§4.5`), `SMTP` pasa a ser un valor válido del enum `Sender`. Con autorización expresa del owner, se actualizó únicamente el literal de la línea 168 de `"smtp"` a `"desconocido"` para que `WI-08` siga verificando el rechazo de un `sender` desconocido en el enum, mientras que el cableado de `sender=smtp` se prueba en `SmtpNotificationWiringTest` (`WI-S01`..`WI-S05`).

### 10.4 Resolución de `TEST_CONTRACT_CONFLICT` en `NotificacionOutboxDispatcherTest#setUp` (Fase 4)
- **Archivo y línea:** `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/notification/outbox/NotificacionOutboxDispatcherTest.java`, método `setUp()`, línea 52.
- **Motivo y decisión del owner:** En `NOTIF-002A` (`CONTRACT §4.3`), el SPI `NotificacionChannelSender` incorpora `default boolean disponible() { return true; }` y `NotificacionOutboxDispatcher#ejecutarCiclo()` verifica `if (!sender.disponible()) return;`. Como `mock(NotificacionChannelSender.class)` en Mockito (`RETURNS_DEFAULTS`) no ejecuta métodos `default` de interfaz y devuelve `false` para cualquier método `boolean` no configurado, con autorización expresa del owner se agregó la línea `when(sender.disponible()).thenReturn(true);` en `setUp()` para que el mock del fixture refleje el valor por defecto `true` del contrato sin alterar ninguna aserción preexistente, mientras `BE-K04` sobrescribe `when(sender.disponible()).thenReturn(false)` en su propio cuerpo.

### 10.5 Precisión de endurecimiento de `esCorreoSeguro` y reutilización en propiedades y canal SMTP
- **Fecha:** 2026-10-30
- **Ámbito:** Backend — `SmtpNotificationProperties#esCorreoSeguro` y `SmtpNotificacionChannelSender#esCorreoSeguro`.
- **Precisión (no desvío):** El criterio de validación de direcciones de correo (`esCorreoSeguro`) se centraliza en `SmtpNotificationProperties.esCorreoSeguro(String)` y se reutiliza en `SmtpNotificacionChannelSender`, `from-address` y `test-mode.redirect-to`, rechazando además de nulos, blancos, espacios y saltos de línea (`\r`, `\n`), los caracteres separadores o delimitadores `,`, `;`, `<`, `>`, `"`, `(` y `)` (evitando que una dirección contenga múltiples destinatarios o sintaxis RFC 5322 compuesta).


