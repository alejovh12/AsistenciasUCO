---
status: active
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-10-28
---

# ADR-005 — Capacidad de notificaciones con proveedor intercambiable (Log, Outbox, Composite)

- **Estado:** accepted
- **Fecha:** 2026-10-28
- **Work item:** [NOTIF-001](../work-items/NOTIF-001-proveedor-notificaciones/PLAN.md)
- **Refina:** [Estándar de Composition Root](../architecture/adapter-composition-standard.md)

## Contexto

El sistema de gestión de asistencia UCO requería notificar eventos académicos (p. ej. retiro de estudiantes de grupos en HU051) a usuarios destinatarios. La implementación inicial consistía en un único `LogNotificacionAdapter` registrado como fallback en `NotificacionAdapterConfiguration` con `@ConditionalOnMissingBean`.

Esta arquitectura presentaba tres limitaciones:
1. Sin durabilidad: las notificaciones eran volátiles y se perdían ante fallos de proceso.
2. Sin capacidad de entrega garantizada ni reintentos ante caídas transitorias de canales.
3. Imposibilidad de seleccionar proveedores alternativos de forma determinística y tipada sin tocar el código de negocio.

## Decisión

1. **Preservación estricta de puertos de negocio:** El puerto secundario `NotificacionPort` y el record de dominio `Notificacion` no cambian ni conocen la tecnología de entrega.
2. **Tres proveedores seleccionables:**
   - `log`: adaptador síncrono que escribe en logs estructurados (modo por defecto para desarrollo/pruebas).
   - `outbox`: persistencia transaccional en `dbo.NotificacionSaliente` mediante stored procedures dedicados (`usp_registrar_notificacion_saliente`, `usp_reservar_notificaciones_salientes`, `usp_marcar_notificacion_entregada`, `usp_registrar_fallo_notificacion`, `usp_depurar_notificaciones_salientes`). Un despachador autónomo (`NotificacionOutboxDispatcher`) procesa lotes concurrentes con lease distribuido, reintentos con backoff exponencial y entrega a un SPI `NotificacionChannelSender` (v1: `LogNotificacionChannelSender`).
   - `composite`: fan-out aislado hacia 2 o más adaptadores hoja configurados (`app.adapters.notification.composite.targets=log,outbox`), garantizando que fallos en un destino no bloqueen al resto.
3. **Selección declarativa en Composition Root:**
   - La selección se realiza exclusivamente en `app.adapters.notification.provider=log|outbox|composite`.
   - Se utilizan condiciones declarativas personalizadas (`LogAdapterCondition`, `OutboxAdapterCondition`) con `@ConditionalOnProperty`.
   - Las propiedades se tipan y validan estrictamente en el arranque mediante `NotificationAdapterProperties` y `OutboxNotificationProperties`.

## Alternativas descartadas

1. **Puerto genérico de mensajería (Enterprise Event Bus):**
   - Descartado porque las notificaciones a usuarios (asunto, cuerpo, destinatarios con rol) son una capacidad de aplicación con semántica diferente a los eventos de dominio o mensajería técnica interna. Crear un bus genérico diluía la cohesión del dominio.
2. **Outbox transaccional fusionado en la transacción de negocio:**
   - Descartado porque la arquitectura de UCO utiliza una Smart Database con procedimientos almacenados independientes. El registro de la notificación se ejecuta mediante el SP público `usp_registrar_notificacion_saliente` preservando la atomicidad ACID sin requerir transacciones distribuidas XA ni acoplamiento de tablas de negocio con la infraestructura de envío.
3. **Selección mediante `@ConditionalOnMissingBean`:**
   - Descartado porque viola el principio de fail-closed y determinismo arquitectural. Una configuración errónea en producción debe fallar el arranque inmediatamente en lugar de activar silenciosamente un adaptador de logging en memoria.

## Consecuencias y límites

- Cumplimiento total de Clean Architecture: Application y Domain permanecen 100% aislados de detalles de persistencia y despacho.
- Garantía de no duplicidad de despacho y concurrencia multi-instancia mediante reservas con bloqueos de base de datos (`WITH (UPDLOCK, READPAST, ROWLOCK)`).
- Pruebas E2E de notificaciones no requieren infraestructura externa (SMTP/SMS) en esta fase: el canal v1 delega a logging tipado y estructurado, permitiendo incorporar proveedores externos en fases posteriores implementando únicamente `NotificacionChannelSender`.
