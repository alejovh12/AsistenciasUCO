package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

/**
 * SPI para el envio de notificaciones a traves de un canal especifico (correo, log, etc.).
 */
public interface NotificacionChannelSender {

    /**
     * Envia la notificacion. Debe ser idempotente por {@code notificacion.id()}.
     *
     * @param notificacion notificacion a entregar
     * @throws NotificacionNoEntregableException si el fallo es definitivo (no reintentable)
     */
    void enviar(NotificacionPersistida notificacion) throws NotificacionNoEntregableException;
}
