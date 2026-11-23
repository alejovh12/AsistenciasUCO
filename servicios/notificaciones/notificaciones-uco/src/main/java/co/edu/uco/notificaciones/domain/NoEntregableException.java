package co.edu.uco.notificaciones.domain;

/** Fallo permanente: reintentar no cambia el resultado (rechazo SMTP 5xx, sin destinatarios validos, tipo retirado). */
public class NoEntregableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoEntregableException(final String message) {
        super(message);
    }
}
