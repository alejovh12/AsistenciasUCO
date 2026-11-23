# Guía de integración del Servicio de Notificaciones UCO

> Versión 0.1.1 · Contrato: [`contrato/openapi-notificaciones.yaml`](contrato/openapi-notificaciones.yaml) (con su `.sha256`) ·
> Esta guía es para cualquier aplicación que necesite enviar correos institucionales. No hace falta conocer la implementación.

## 1. Qué hace el servicio

Recibe **avisos** (un tipo, los destinatarios ya resueltos y los datos de la plantilla) y se encarga de todo lo demás:

- **Plantillas versionadas** por tipo e idioma, con marcadores permitidos (`{nombre}`), vista previa y activación.
- **Renderizado** del asunto y del cuerpo (texto plano y HTML con el layout de la UCO), con el HTML siempre escapado.
- **Cola propia** con reserva (lease) para que dos instancias no envíen el mismo aviso, **reintentos con backoff**
  exponencial y registro de entregas.
- **Tope de envíos** por minuto y por día, correo de prueba y estado del canal.
- **Modo de pruebas**: redirige todos los correos a buzones de prueba y avisa en el asunto y en el cuerpo.

Lo que **no** hace: resolver quién es el destinatario. La aplicación cliente entrega los correos ya resueltos.

## 2. Conexión y seguridad

| Dato | Valor en el laboratorio |
|---|---|
| URL desde el PC | `http://localhost:18090` |
| URL desde otro contenedor (red `asis-red`) | `http://ext-notificaciones-servicio:8080` |
| Autenticación | Cabecera `X-Api-Key: <clave>` en todo `/api/v1/**` y en `/actuator/prometheus` |
| Salud (abierta) | `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` |

- La clave es un secreto compartido que vive en el `.env` de Infra (`NOTIFICACIONES_API_KEY`); **nunca** en el repositorio.
  Sin clave o con una clave incorrecta la respuesta es `401` con `{"code":"NO_AUTORIZADO",...}`.
- Deuda técnica registrada: pasar a OAuth2 client-credentials cuando Seguridad lo ofrezca para servicios.

### Cabeceras de correlación

| Cabecera | Uso |
|---|---|
| `X-Correlation-Id` | Opcional. Hasta 64 caracteres `[A-Za-z0-9_.-]`. Si falta o no es válido el servicio genera un UUID. Se devuelve siempre y aparece como `correlationId` en los logs JSON y en los errores. |
| `traceparent` | Opcional (W3C Trace Context). Se guarda con el aviso y propaga la traza distribuida. |

## 3. Enviar un aviso — `POST /api/v1/notificaciones`

```http
POST /api/v1/notificaciones
X-Api-Key: <clave>
X-Correlation-Id: 6f1c0f0e-...            (opcional)
Content-Type: application/json

{
  "aplicacion": "asistencias",
  "idempotencyKey": "00000000-0000-0000-0000-000000000001",
  "tipo": "SESION_CANCELADA",
  "idioma": "es",
  "destinatarios": [ { "correo": "ana.gomez@uco.edu.co", "nombre": "Ana Gómez" } ],
  "datos": {
    "estudiante": "Ana Gómez", "asignatura": "Bases de Datos", "grupo": "G01",
    "fecha": "2026-11-06", "horaInicio": "08:00", "horaFin": "10:00",
    "motivo": "Incapacidad médica", "docente": "María Rostagno"
  },
  "asunto": "Clase cancelada",
  "mensaje": "El docente canceló la clase.",
  "idCorrelacion": "6f1c0f0e-...",
  "traceparent": "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
}
```

| Campo | Obligatorio | Reglas |
|---|---|---|
| `aplicacion` | sí | Hasta 60 caracteres `[A-Za-z0-9_.-]`. Identifica a quién origina el aviso. |
| `idempotencyKey` | sí | Hasta 120 caracteres `[A-Za-z0-9_.:@-]`. **Misma `aplicacion` + misma clave = el mismo envío.** Asistencias usa el id de su `NotificacionSaliente`. |
| `tipo` | sí | Uno de los tipos de la sección 5 (`GET /api/v1/plantillas`). |
| `destinatarios` | sí | De 1 a 200. Cada `correo` debe ser una sola dirección válida (sin espacios, comas, punto y coma, comillas ni paréntesis). Los repetidos se envían una vez. |
| `datos` | no | Hasta 100 pares `marcador → valor`. Claves alfanuméricas (hasta 40) y valores de hasta 2000 caracteres. |
| `idioma` | no | Dos letras minúsculas. Por defecto `es`. |
| `asunto`, `mensaje` | no | Texto de **respaldo** (hasta 200 y 4000): se usa solo si no hay plantilla utilizable para los datos recibidos. |
| `idCorrelacion`, `traceparent` | no | Si no vienen, se toman de las cabeceras. |

### Respuestas

| Estado | Significado | ¿Qué hace el cliente? |
|---|---|---|
| `202` | Aviso nuevo aceptado (`estado: PENDIENTE`, `duplicada: false`). | Dar el aviso por entregado **a la cola**; el envío es asíncrono. |
| `200` | La clave ya existía: misma `id`, `duplicada: true`, **no** se envía otra vez. | Igual que `202`. |
| `4xx` | El aviso es inválido (`TIPO_NO_SOPORTADO`, `DESTINATARIO_INVALIDO`, `VALIDACION_FALLIDA`...) o la clave API no sirve. | **No reintentar**: marcar fallido con el `code` recibido. |
| `5xx` o tiempo de espera agotado | El servicio no pudo registrar el aviso. | Reintentar **con la misma `idempotencyKey`** (con backoff). Nunca genera duplicados. |

> Cuerpo de error uniforme: `{"code": "TIPO_NO_SOPORTADO", "message": "…", "correlationId": "…"}`. El `code` es estable;
> el `message` está listo para mostrar.

### Qué pasa después

El aviso pasa por `PENDIENTE → RESERVADA → ENTREGADA`. Si el SMTP falla de forma transitoria queda `FALLIDA_REINTENTABLE` y se
reintenta con backoff `min(30 s × 2^(intento−1), 15 min)` hasta 5 intentos; un rechazo permanente (SMTP 5xx, sin destinatarios
válidos, tipo retirado) queda `FALLIDA_DEFINITIVA`. Las entregas terminales se conservan 30 días.

## 4. Consultar y operar la cola

| Operación | Endpoint |
|---|---|
| Cola paginada | `GET /api/v1/notificaciones?estado=&tipo=&aplicacion=&pagina=1&tamano=20` → `{elementos, numeroPagina, tamanoPagina, totalElementos, totalPaginas, resumen}` |
| Reintentar | `POST /api/v1/notificaciones/{id}/reintento` → `202`; `404` si no existe; `409` `ESTADO_NO_REINTENTABLE` si no está fallida |
| Correo de prueba | `POST /api/v1/correos-prueba` con `{"tipo":"…","correoDestino":"…","solicitadoPor":"…"}` → `202` (sin `tipo` envía el correo genérico del canal) |
| Estado del canal | `GET /api/v1/estado-canal` → configuración visible, `canalActivo`, `ultimoEnvioUtc`, `ultimoError` (nunca la clave del SMTP) |

La cola **no devuelve correos ni datos** de los avisos: solo `cantidadDestinatarios`.

## 5. Plantillas

`GET /api/v1/plantillas?idioma=es` devuelve, por tipo: `descripcion` (texto para tu pantalla de administración; **no lo
quemes en el cliente**), `marcadores` permitidos (con `descripcion`, `ejemplo` y `obligatorio`), la versión activa y el historial.

| Operación | Endpoint |
|---|---|
| Crear versión (queda **activa**) | `POST /api/v1/plantillas/{tipo}/versiones` con `{"idioma":"es","asunto":"…","cuerpo":"…","creadoPor":"…"}` → `201` |
| Volver a una versión anterior | `POST /api/v1/plantillas/{tipo}/versiones/{n}/activacion?idioma=es` → `200` |
| Vista previa de un borrador | `POST /api/v1/plantillas/vista-previa` con `{"tipo","idioma","asunto","cuerpo"}` → asunto, texto plano y HTML renderizados con los ejemplos de los marcadores |

Reglas al crear una versión: el asunto tiene hasta 200 caracteres y el cuerpo hasta 4000; deben aparecer **todos** los marcadores
obligatorios; solo se permiten marcadores del tipo; las llaves deben estar balanceadas. La versión 1 es la **de fábrica** y sirve de
respaldo si la versión activa no puede renderizarse con los datos recibidos (por ejemplo, falta un dato obligatorio).

### Tipos y marcadores de fábrica (`*` = obligatorio)

| Tipo | Descripción | Marcadores |
|---|---|---|
| `ACTIVACION_CUENTA` | Activación de cuenta: aviso a una persona registrada por otra (decano, coordinador, docente o estudiante) para definir su contraseña con un enlace de un solo uso. | `nombre*`, `enlace*`, `rol*` |
| `ALERTA_PERDIDA_INASISTENCIA` | Alerta de pérdida de asignatura: el estudiante superó el límite institucional de inasistencia. | `estudiante*`, `grupo*`, `porcentaje*`, `limite` |
| `ALERTA_RIESGO_INASISTENCIA` | Alerta de riesgo: el estudiante se acerca al límite institucional de inasistencia. | `estudiante*`, `grupo*`, `porcentaje*`, `umbral`, `limite` |
| `CANCELACION_APROBADA` | Cancelación por inasistencia aprobada: aviso al estudiante y al docente titular. | `destinatario*`, `estudiante*`, `grupo*`, `porcentaje*`, `motivo*`, `resolutor*` |
| `CANCELACION_RECHAZADA` | Propuesta de cancelación rechazada: aviso al docente titular; el estudiante sigue activo. | `docente*`, `estudiante*`, `grupo*`, `motivo*`, `resolutor*` |
| `CIERRE_MASIVO_PERIODO_EJECUTADO` | Cierre masivo del período académico ejecutado por el administrador institucional. | `periodo*`, `totalEstudiantes`, `totalAprobados`, `totalReprobados` |
| `FALTA_ASISTENCIA_SESION_ESTUDIANTE` | Aviso al estudiante de las tomas en falta de una sesión terminada, con el plazo para reclamar. | `estudiante*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `detalleFaltas*`, `fechaLimiteReclamo*`, `enlaceReclamo*` |
| `PROPUESTA_CANCELACION` | Propuesta de cancelación por inasistencia: aviso al coordinador (o al decano por escalamiento). | `resolutor*`, `estudiante*`, `grupo*`, `docente*`, `porcentaje*`, `motivo*`, `limite`, `escalamiento` |
| `RECESO_CLASES_CANCELADAS_DOCENTE` | Receso académico: resumen al docente titular de sus clases canceladas ese día. | `docente*`, `fecha*`, `motivo*`, `clases*` |
| `RECESO_CLASES_CANCELADAS_ESTUDIANTE` | Receso académico: resumen al estudiante de las clases canceladas ese día. | `estudiante*`, `fecha*`, `motivo*`, `clases*` |
| `RECLAMO_ASISTENCIA_RADICADO` | Solicitud de revisión de asistencia radicada por el estudiante: aviso al docente titular. | `grupo*`, `estudiante*`, `fecha*`, `motivo` |
| `RECLAMO_ASISTENCIA_RESUELTO` | Solicitud de revisión de asistencia resuelta por el docente: aviso al estudiante. | `grupo*`, `fecha*`, `estado*`, `observacion` |
| `RETIRO_ESTUDIANTE_GRUPO` | Retiro de un estudiante del grupo por el docente titular. | `grupo*`, `estudiante*`, `motivo` |
| `REVISION_CANCELACION_PENDIENTE` | Revisión de cancelación por inasistencia pendiente: aviso al docente titular. | `docente*`, `estudiante*`, `grupo*`, `porcentaje*`, `limite` |
| `SESION_CANCELADA` | Clase cancelada por el docente titular: aviso a cada estudiante activo del grupo. | `estudiante*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `motivo*`, `docente*` |
| `SESION_MANUAL_CREADA_COORDINADOR` | Sesión creada manualmente por el docente: aviso al coordinador del programa. | `coordinador*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `docente*` |
| `SESION_MANUAL_CREADA_ESTUDIANTE` | Sesión creada manualmente por el docente: aviso a cada estudiante activo del grupo. | `estudiante*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `docente*` |
| `SESION_SIN_TOMA_COORDINADOR` | Sesión terminada sin toma de asistencia: aviso informativo al coordinador. | `coordinador*`, `docente*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `enlaceSesiones*` |
| `SESION_SOLICITUD_APROBADA_DOCENTE` | Solicitud de sesión extraordinaria aprobada por el coordinador: aviso al docente titular. | `docente*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `coordinador*` |
| `SESION_SOLICITUD_CREADA_COORDINADOR` | Solicitud de sesión extraordinaria del docente: aviso a los coordinadores del programa. | `coordinador*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `docente*` |
| `SESION_SOLICITUD_RECHAZADA_DOCENTE` | Solicitud de sesión extraordinaria rechazada: aviso al docente titular con motivo y observación. | `docente*`, `asignatura*`, `grupo*`, `fecha*`, `horaInicio*`, `horaFin*`, `coordinador*`, `motivo*`, `observacion*` |
| `VERIFICACION_CORREO` | Verificación de correo: aviso al estudiante registrado por el docente, con un enlace de un solo uso. | `nombre*`, `enlace*`, `grupo` |

El marcador `enlace` se muestra como vínculo solo si su valor es una URL `http` o `https`; cualquier otro valor se escapa como texto.

## 6. Observabilidad

- **Logs**: JSON por la salida estándar (formato logstash) con `correlationId`, `traceId`, `spanId` y `service`. Nunca incluyen
  correos, asuntos ni cuerpos.
- **Métricas** (`GET /actuator/prometheus`, con `X-Api-Key`): `notification_dispatch_total{outcome}`,
  `notification_email_send_total{outcome}`, `notification_email_throttled_total{window}`, `notification_template_render_total{origin}`,
  `notificaciones_recibidas_total{resultado}`, además de JVM y HTTP.
- **Trazas**: OTLP por HTTP (apagado por defecto; `MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED=true` y el endpoint del colector).

## 7. Límites y valores por defecto

> **Avisos agrupados:** un aviso con varios destinatarios se renderiza una sola vez y se entrega en un mensaje con BCC; los
> datos son comunes y el saludo debe ser genérico (p. ej. `estudiante: "estudiantes del grupo"`). No existe renderizado por
> destinatario; para personalizar, emita un aviso por persona dentro del tope de envíos.


| Parámetro | Valor | Variable |
|---|---|---|
| Destinatarios por aviso | 200 | — |
| Destinatarios por mensaje SMTP (BCC) | 50 | `SMTP_NOTIFICATION_MAX_BCC` |
| Tope de envíos | 60/min y 500/día | `SMTP_NOTIFICATION_RATE_PER_MINUTE`, `SMTP_NOTIFICATION_RATE_PER_DAY` |
| Lote del despachador / lease | 50 / 60 s | `DISPATCHER_BATCH_SIZE`, `DISPATCHER_LEASE` |
| Barrido de la cola | 30 s (o al instante al llegar un aviso) | `DISPATCHER_SWEEPER_INTERVAL` |
| Intentos y backoff | 5, base 30 s, máximo 15 min | `DISPATCHER_MAX_ATTEMPTS`, `DISPATCHER_BACKOFF_BASE`, `DISPATCHER_BACKOFF_MAX` |
| Retención de entregas terminales | 30 días | `DISPATCHER_RETENTION_DAYS` |

## 8. Reemplazar este componente

Cualquier otro servicio de notificaciones que cumpla [`contrato/openapi-notificaciones.yaml`](contrato/openapi-notificaciones.yaml)
puede ocupar su lugar sin cambiar a las aplicaciones: basta apuntar la URL y la clave. El contrato está congelado con su `.sha256`; todo
cambio exige un addendum y un hash nuevo.
