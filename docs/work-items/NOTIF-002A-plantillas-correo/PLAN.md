---
status: frozen-approved
type: plan
scope: database-backend
last-reviewed: 2026-10-29
---

# PLAN — NOTIF-002A: Plantillas de notificación y envío de correo (SMTP) sobre el outbox

Estado: **`FROZEN`** (aprobado por el owner el 2026-10-29 con D1–D14 y los defaults de Q1–Q5). Depende de NOTIF-001 (outbox, despachador y SPI `NotificacionChannelSender`), hoy en commits locales de la rama `feature/NOTIF-001-notificaciones-outbox`.

## 1. Objetivo

NOTIF-001 dejó la capacidad de notificaciones durable e intercambiable, pero el único canal (`LogNotificacionChannelSender`) solo escribe en el log. NOTIF-002A agrega, **dentro del mismo backend y como módulo removible**:

1. **Envío real de correo** por SMTP genérico (`SmtpNotificacionChannelSender`), seleccionable con `app.providers.outbox-notification.sender=smtp` (default `log`).
2. **Plantillas con contenido editable**: el contenido (asunto y cuerpo) por `tipo` vive en la base de datos con versionado; la **estructura HTML es fija** y vive en el backend. La edición por el Administrador (pantalla y endpoints) es **NOTIF-002B**, fuera de este work item; aquí solo el modelo, la lectura y las plantillas semilla.
3. **Resolución del contacto en el momento de la entrega**: el outbox sigue guardando solo `rol` + `idUsuario`; el correo se consulta al entregar y no se persiste.
4. Tope de envíos, clasificación de errores y métricas, sin proveedores de pago ni dependencias externas: el SMTP lo provee la universidad (servidor propio); en desarrollo y CI se usan Mailpit y GreenMail.

## 2. Estado actual verificado (2026-10-29)

| Hecho | Evidencia |
| :-- | :-- |
| El SPI de canal ya existe y distingue fallo permanente de transitorio. | `NotificacionChannelSender`, `NotificacionNoEntregableException` (NOTIF-001). |
| El despachador reserva filas, llama al sender y gestiona reintentos y dead-letter. | `NotificacionOutboxDispatcher` (NOTIF-001). |
| `Usuario.correo` (`nvarchar(100) NOT NULL`) y `correoConfirmado` existen. | `GestioAsistenciaDB/schema/tables/Usuario.sql`. |
| Cuatro casos de uso emiten avisos: `RETIRO_ESTUDIANTE_GRUPO` (HU051), `SOLICITUD_INGRESO_REGISTRADA`, `SOLICITUD_INGRESO_RESUELTA`, `SOLICITUD_INGRESO_RETIRADA` (HU053/054/055). | `Notificacion.TIPO_*`; `RetirarEstudianteGrupoUseCaseImpl`, `SolicitarIngresoGrupoUseCaseImpl`, `ResolverSolicitudIngresoUseCaseImpl`, `RetirarSolicitudIngresoUseCaseImpl`. |
| `Notificacion.datos` hoy lleva solo identificadores (y `motivo` en HU051); el texto legible ya viene armado en `asunto`/`mensaje` dentro del caso de uso. | `RetirarEstudianteGrupoUseCaseImpl#construirAviso`. |
| El outbox ya persiste `asunto` y `mensaje` (que contienen nombres de persona) con retención de 30 días. | NOTIF-001 D5. |
| No hay dependencia de correo en el `pom.xml`. | `pom.xml` (sin `spring-boot-starter-mail`). |

## 3. Alcance

**Dentro**
- DB: tablas `PlantillaNotificacion` (versionada) y `PlantillaNotificacionMarcador` (catálogo de marcadores por tipo), plantillas semilla de los 4 tipos, SP técnicos de lectura de plantilla activa y de contacto.
- Backend (módulo `notification`, sub-paquetes `template`, `contact`, `channel.smtp`): renderizador con escape, layout HTML fijo + alternativa de texto plano, resolución de contacto, sender SMTP, tope de envíos, método `disponible()` del sender, propiedades, wiring y métricas.
- Enriquecer `Notificacion.datos` en los 4 casos de uso con los valores legibles (solo agregar claves; `asunto`/`mensaje` actuales no cambian y son el último respaldo).
- Pruebas (T-SQL, unitarias, ArchUnit, GreenMail, E2E con Mailpit) y documentación (ADR-006, estándar de composición, deuda técnica).

**Fuera (seguimientos, §7)**
- **NOTIF-002B:** endpoints y pantalla de administración de plantillas (listar, editar, vista previa, activar/revertir versión) y su permiso en el catálogo de accesos.
- Otros canales (SMS, WhatsApp, push); preferencias por usuario; bandeja de notificaciones en la web; adjuntos.
- Proveedor externo o componente de otro equipo (el diseño lo permite: ver ADR-006 y CONTRACT §6).

## 4. Decisiones (D1–D14)

| ID | Decisión | Recomendación |
| :-- | :-- | :-- |
| D1 | Todo vive **dentro del backend** como módulo aislado `notification` (sub-paquetes `outbox`, `template`, `contact`, `channel.smtp`). Solo `config` puede instanciar clases concretas; el resto del sistema solo conoce `NotificacionPort`. | Sí |
| D2 | `NotificacionPort`/`Notificacion` **no cambian de firma**. Los casos de uso solo **agregan claves** a `datos` para alimentar plantillas. `asunto` y `mensaje` actuales se conservan como último respaldo. | Sí |
| D3 | Orden de resolución de contenido al entregar: (1) plantilla activa en BD, (2) plantilla por defecto del backend (recurso versionado), (3) `asunto`/`mensaje` guardados en la fila. Si faltan marcadores obligatorios en `datos`, se baja al siguiente nivel y se cuenta `origin=fallback`. | Sí |
| D4 | **Contenido editable, estructura fija.** La plantilla en BD es texto plano con marcadores `{nombre}`. El backend lo inserta en un layout HTML fijo (cabecera, pie institucional) y genera también la alternativa `text/plain`. El administrador no edita HTML. | Sí |
| D5 | Marcadores controlados: cada tipo declara en `PlantillaNotificacionMarcador` los marcadores válidos y cuáles son obligatorios. Un marcador desconocido se rechaza al guardar (002B) y, si aparece al renderizar, se trata como error de plantilla (cae a respaldo). | Sí |
| D6 | **Escape siempre**: cada valor sustituido se escapa para HTML; el contenido se transforma a párrafos y saltos de línea; no se interpretan enlaces ni HTML del texto de la plantilla. | Sí |
| D7 | El correo del destinatario se resuelve **al entregar** con SP técnico `usp_consultar_contacto_notificacion` a partir de `idUsuario`; no se persiste en el outbox. Usuario sin correo o inhabilitado → destinatario omitido; sin ningún destinatario válido → `NotificacionNoEntregableException` (permanente). | Sí |
| D8 | Un único mensaje SMTP por notificación, con los destinatarios en **BCC** (en bloques de `max-bcc`, default 50) y `To` = remitente institucional. Fallo parcial entre bloques → reintento de la fila completa (entrega *at-least-once*, documentada). | Sí |
| D9 | Clasificación de errores SMTP: permanentes (dirección inválida, 5xx, plantilla/configuración inválida) → `NotificacionNoEntregableException`; transitorios (conexión, timeout, 4xx) → excepción no controlada que el despachador reintenta con su backoff. | Sí |
| D10 | **Tope de envíos** en el sender (`rate-per-minute` default 60, `rate-per-day` default 500). Cuando se agota, el sender expone `disponible()=false` y el despachador **no reserva filas ni consume intentos** hasta que haya cupo (cambio mínimo y compatible en el SPI: método `default boolean disponible() { return true; }`). | Sí |
| D11 | Selector del canal: `app.providers.outbox-notification.sender` = `log` (default) \| `smtp`. Con `smtp`, el arranque falla si faltan `host`, `port` o `from-address` (fail-fast). Credenciales **nunca** en el repositorio: usuario/clave por variable de entorno o `SecretVaultPort` (el implementador verifica el patrón vigente y lo replica). | Sí |
| D12 | **Privacidad del contenido (default):** el `motivo` del retiro **no** va en el cuerpo de la plantilla semilla (el marcador `{motivo}` existe y es válido, pero la plantilla semilla no lo usa); el correo es un aviso breve que remite a la plataforma. El owner puede cambiarlo editando la plantilla (002B). | Sí (decisión del owner: ver §9) |
| D13 | Datos y logs: ningún log incluye correos, asunto ni cuerpo; solo `idNotificacion`, `tipo`, conteos y `origin`. Las métricas llevan etiquetas de baja cardinalidad. | Sí |
| D14 | Sin proveedores de pago: dependencia permitida solo `spring-boot-starter-mail` (Jakarta Mail) y, solo en pruebas, GreenMail. Mailpit se usa como contenedor local (no se versiona en este repositorio; su alta en `docker-compose` es un seguimiento de Infra). | Sí |

## 5. Fases (TDD estricto; cada fase termina en commits atómicos, verde y parada)

| Fase | Contenido | Verificación |
| :-- | :-- | :-- |
| 0 | Preparación: `git fetch`, worktrees desde `origin/develop` (o la rama de NOTIF-001 si aún no está integrada), traer los documentos, congelar PLAN/CONTRACT/TEST_PLAN. Baseline de pruebas unitarias e IT. | Cifras de baseline |
| 1 | **DB**: pruebas T-SQL RED → tablas, catálogo de marcadores, plantillas semilla, SP técnicos (plantilla activa, contacto), permisos de ejecución, mensajes, registro en suite. | Gate de BD en verde |
| 2 | **Backend núcleo de plantillas**: pruebas RED → renderizador, escape, layout, respaldo por defecto, repositorio de plantillas, métricas. | `mvnw test` verde + cobertura |
| 3 | **Contacto y casos de uso**: resolución de contacto; agregar claves a `datos` en los 4 casos de uso (sin tocar `asunto`/`mensaje`). | Pruebas de los 4 casos de uso sin cambios en sus aserciones previas |
| 4 | **Sender SMTP**: pruebas RED → `SmtpNotificacionChannelSender`, tope de envíos, `disponible()` en SPI/despachador, propiedades, wiring fail-fast. | Unitarias + IT con GreenMail |
| 5 | **Archivos calientes y docs**: `git merge origin/develop`; `application.yml`, `AdapterPropertiesConfiguration`, ArchUnit, `adapter-composition-standard.md`, ADR-006, deuda técnica. | `mvnw test` + failsafe vs baseline |
| 6 | **E2E** en el puerto 8090 con Mailpit; `VALIDATION.md`. | Correo recibido y renderizado |

Paradas obligatorias: fin de las fases 1, 2, 4 y 6, y ante cualquier conflicto de contrato.

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
| :-- | :-- |
| Agregar claves a `datos` rompe pruebas existentes que comparan el mapa completo. | Si ocurre, es `TEST_CONTRACT_CONFLICT`: se detiene y se consulta; no se relajan aserciones. |
| Duplicados por reintento tras fallo parcial entre bloques BCC. | Documentado (D8); notificaciones de este sistema tienen pocos destinatarios (bloque único). |
| Plantilla editada con errores deja el correo ilegible. | Marcadores validados (D5) y descenso a respaldo (D3); vista previa en 002B. |
| El servidor SMTP institucional exige TLS/autenticación particular. | Propiedades `starttls`, `ssl`, `username`; probadas contra Mailpit/GreenMail; ajuste fino cuando TI entregue datos. |
| Cuenta de remitente limitada (si se usa una cuenta con topes bajos). | Tope de envíos (D10) configurable. |
| El correo del usuario cambia entre el aviso y la entrega. | Se resuelve al entregar (D7); es el comportamiento deseado. |
| Retiro futuro del módulo. | D1 + reglas ArchUnit; ver CONTRACT §6. |

## 7. Seguimientos (se registran como deuda técnica al cerrar; números = siguientes libres tras recontar)

1. **NOTIF-002B** (HU de administración de plantillas): DB (SP de guardado, activación y consulta paginada), backend (endpoints, permiso en `access-catalog.json`) y frontend (Signals, OnPush).
2. Alta de Mailpit en `docker-compose` de Infra y documentación de su uso.
3. Credenciales SMTP en el almacén de secretos definitivo cuando TI las entregue.
4. SPF/DKIM/DMARC del dominio remitente (coordinación con TI).
5. Exportación de plantillas (volcado) para migrar a otro componente.
6. Idempotencia de entrega por bloque (evitar duplicados en reintentos parciales).

## 8. Criterios de cierre

- Todas las pruebas de TEST_PLAN en verde con cifras reales (DB gate, unitarias, failsafe contra baseline, E2E).
- Correo de los 4 tipos recibido y legible en Mailpit; HTML escapado; alternativa de texto plano presente; sin PII en logs.
- ArchUnit del módulo en verde; `NotificacionPort`/`Notificacion` sin cambios de firma.
- Sin cambios en `SecurityConfig`, `access-catalog.json` ni OpenAPI (no hay endpoints en 002A).
- Documentación actualizada y deuda técnica registrada. Sin push ni PR hasta orden explícita del owner.

## 9. Decisiones del owner (aprobadas con los defaults propuestos el 2026-10-29)

| ID | Pregunta | Default propuesto |
| :-- | :-- | :-- |
| Q1 | ¿El `motivo` del retiro puede ir en el cuerpo del correo? | No (D12): aviso breve con enlace a la plataforma |
| Q2 | Tope de envíos | 60/min y 500/día (configurable) |
| Q3 | ¿Enviar solo a correos confirmados (`correoConfirmado=1`)? | No filtrar por defecto; propiedad `require-confirmed-email=false` |
| Q4 | Datos del SMTP institucional (host, puerto, TLS, remitente) | Pendiente de TI (no bloquea 002A); hasta entonces Mailpit/GreenMail |
| Q5 | URL base de la plataforma para el enlace del correo | Propiedad `app.providers.smtp-notification.platform-url` (sin valor por defecto en producción) |
