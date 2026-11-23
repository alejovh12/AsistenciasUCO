package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

/**
 * El Servicio de Notificaciones respondio con un estado fuera de 2xx. Lleva el estado HTTP y el {@code code} estable del cuerpo
 * de error uniforme {@code {code, message, correlationId}}; el {@code message} del servicio esta listo para mostrar.
 */
public final class ServicioNotificacionesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;
    private final String mensajeServicio;

    public ServicioNotificacionesException(final int status, final String code, final String mensajeServicio) {
        super("Servicio de notificaciones respondio HTTP " + status + (code == null || code.isBlank() ? "" : " (" + code + ")"));
        this.status = status;
        this.code = code == null ? "" : code;
        this.mensajeServicio = mensajeServicio == null ? "" : mensajeServicio;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String mensajeServicio() {
        return mensajeServicio;
    }
}
