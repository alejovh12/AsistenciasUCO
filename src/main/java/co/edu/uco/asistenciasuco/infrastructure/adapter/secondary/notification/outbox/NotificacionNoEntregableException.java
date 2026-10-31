package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

/**
 * Excepcion que senala que una notificacion no puede ser entregada de forma definitiva.
 */
public class NotificacionNoEntregableException extends Exception {

    public NotificacionNoEntregableException(final String message) {
        super(message);
    }

    public NotificacionNoEntregableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
