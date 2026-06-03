package co.edu.uco.asistenciasuco.crosscutting.exception;

/**
 * Excepción base para errores técnicos/utilitarios de la capa crosscutting.
 */
public class CrosscuttingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CrosscuttingException(final String message) {
        super(message);
    }

    public CrosscuttingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
