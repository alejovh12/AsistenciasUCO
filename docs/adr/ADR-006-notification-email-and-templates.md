---
status: draft
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-10-29
---

# ADR-006 — Envío de correo y plantillas de notificación como módulo removible

- **Estado:** proposed (diseño aprobado por el owner el 2026-10-29; pasa a accepted al cerrar la Fase 5)
- **Fecha:** 2026-10-29
- **Work item:** [NOTIF-002A](../work-items/NOTIF-002A-plantillas-correo/PLAN.md)
- **Extiende:** [ADR-005](ADR-005-notification-provider-switch.md)

## Contexto

NOTIF-001 dejó el outbox durable y un SPI de canal (`NotificacionChannelSender`) con un único sender que escribe en el log. El equipo que iba a entregar el componente institucional de correo y notificaciones **se retiró del proceso**, así que el correo debe construirse dentro de este proyecto. Restricciones del owner:

1. La universidad despliega todo en sus propios servidores y **no autoriza proveedores externos de pago**.
2. Un **administrador** debe poder modificar el **contenido** de los correos, no su estructura HTML.
3. El diseño debe permitir **retirar el módulo** y consumir un componente de otro equipo en el futuro, igual que hoy se cambia el proveedor de seguridad.
4. Azure App Configuration y Key Vault se mantienen por ahora (otros equipos entregan esas herramientas); depender de ellos para las plantillas impediría la edición por un administrador desde la plataforma.

## Decisión

1. **Módulo dentro del backend, no un servicio aparte.** Código bajo `infrastructure.adapter.secondary.notification.{outbox,template,contact,channel.smtp}`; solo `config` instancia clases concretas; el resto del sistema conoce únicamente `NotificacionPort`. Reglas ArchUnit lo hacen verificable.
2. **Transporte: SMTP genérico** (Jakarta Mail vía `spring-boot-starter-mail`) detrás del SPI `CorreoTransport` y del sender `smtp`. Cambiar de servidor (Mailpit, GreenMail, SMTP institucional, un servidor de pruebas con cuenta propia) es solo configuración. No hay SDK de proveedor.
3. **Plantillas en SQL Server**, versionadas, con **contenido en texto plano y marcadores controlados** (`{grupo}`, `{estudiante}`…) y **estructura HTML fija** en el backend. Orden de resolución al entregar: plantilla activa en BD → plantilla por defecto del backend → `asunto`/`mensaje` guardados. La edición por el Administrador (endpoints y pantalla) es **NOTIF-002B**.
4. **Contacto al entregar:** el correo se consulta por `idUsuario` en el momento del envío y no se persiste en el outbox (se conserva la minimización de NOTIF-001).
5. **Tope de envíos** en el sender, expuesto al despachador mediante `disponible()` (método con implementación por defecto en el SPI): sin cupo no se reservan filas ni se consumen intentos.
6. **Privacidad por defecto:** el correo es un aviso breve; el motivo del retiro no se incluye en la plantilla semilla.

## Alternativas descartadas

| Alternativa | Motivo |
| :-- | :-- |
| Plantillas en Azure App Configuration | Sin pantalla de edición propia para el Administrador; la universidad despliega en sus servidores y App Configuration es una dependencia externa que otros equipos aún entregan. |
| Plantillas con motor completo (Thymeleaf/FreeMarker) en BD | Permite que el administrador inyecte lógica o HTML; mayor superficie de ataque; innecesario para ~4 plantillas con marcadores. |
| Plantillas solo en el repositorio | Cada cambio de texto requeriría un despliegue; no cumple el requisito de edición por el Administrador. |
| SDK de un proveedor concreto (SendGrid, SES, Graph) | Acopla el código a un tercero, con costo o autorización pendiente; SMTP cubre el caso y un SDK futuro entra como otro sender. |
| Microservicio de correo aparte | Despliegue, red y observabilidad extra sin necesidad hoy; el módulo aislado se puede extraer después. |
| Un correo por destinatario | Más conexiones y más duplicados en reintentos; el contenido es idéntico para todos los destinatarios de un aviso (BCC en bloques). |

## Consecuencias y límites

- **Sustituibilidad** (tres niveles): (a) solo el envío → nuevo `NotificacionChannelSender` (p. ej. HTTP hacia el componente del otro equipo); (b) envío y plantillas → el otro componente renderiza y nuestro renderizador no se usa; (c) todo el proveedor → nuevo valor de `app.adapters.notification.provider`; las tablas de outbox y plantillas se retiran con una migración de limpieza. Ningún caso de uso ni sus pruebas cambian.
- **Entrega *at-least-once*:** un fallo parcial entre bloques BCC provoca reintento de toda la fila y posibles duplicados (documentado; pocos destinatarios por aviso).
- **Cambio acotado en casos de uso:** los 4 emisores agregan claves legibles a `Notificacion.datos` (no cambian `NotificacionPort` ni `asunto`/`mensaje`).
- **Pendiente de terceros:** datos del SMTP institucional (host, puerto, TLS, remitente) y SPF/DKIM/DMARC del dominio remitente los entrega TI.
- **Deuda registrada:** NOTIF-002B (administración), Mailpit en `docker-compose` de Infra, credenciales SMTP en el almacén definitivo de secretos, exportación de plantillas, idempotencia por bloque.
