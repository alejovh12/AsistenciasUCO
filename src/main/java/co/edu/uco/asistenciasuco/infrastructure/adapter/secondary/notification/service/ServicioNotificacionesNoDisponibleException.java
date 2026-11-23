package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

/**
 * El Servicio de Notificaciones no se pudo alcanzar: red, tiempo de espera agotado, circuit breaker abierto o servicio sin
 * configurar. Es siempre reintentable.
 */
public final class ServicioNotificacionesNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServicioNotificacionesNoDisponibleException(final String message) {
        super(message);
    }

    public ServicioNotificacionesNoDisponibleException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
