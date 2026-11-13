package co.edu.uco.asistenciasuco.application.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;

import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Excepcion base para errores controlados de aplicacion.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorDefinition errorDefinition;
    private final String code;
    private final String[] messageArgs;

    protected ApplicationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition.defaultMessage());
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
        this.messageArgs = new String[0];
    }

    protected ApplicationException(final ErrorDefinition errorDefinition, final String message) {
        super(message);
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
        this.messageArgs = new String[0];
    }

    protected ApplicationException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
        this.messageArgs = new String[0];
    }

    protected ApplicationException(final String code, final String message) {
        super(message);
        this.errorDefinition = null;
        this.code = code;
        this.messageArgs = new String[0];
    }

    protected ApplicationException(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.errorDefinition = null;
        this.code = code;
        this.messageArgs = new String[0];
    }

    /**
     * Mensaje de una definicion de catalogo con parametros (VAL-oleada3 MSG-05): el texto por defecto lleva marcadores
     * {@code {}} que se llenan con {@code argumentos}; los mismos argumentos llenan el texto del catalogo al responder.
     */
    protected ApplicationException(final ErrorDefinition errorDefinition, final Object[] argumentos) {
        super(TextHelper.reemplazarMarcadores(errorDefinition.defaultMessage(), argumentos));
        this.errorDefinition = errorDefinition;
        this.code = errorDefinition.code();
        this.messageArgs = argumentos == null ? new String[0]
                : Arrays.stream(argumentos).map(String::valueOf).toArray(String[]::new);
    }

    public String getCode() {
        return code;
    }

    /** Argumentos del mensaje de catalogo; vacio cuando la excepcion no usa marcadores. */
    public List<String> getMessageArgs() {
        return List.of(messageArgs);
    }

    public Optional<ErrorDefinition> getErrorDefinition() {
        return Optional.ofNullable(errorDefinition);
    }
}
