package co.edu.uco.asistenciasuco.application.exception.internal;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

import java.util.Objects;

public class InternalApplicationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public InternalApplicationException(final String message) {
        super(CommonErrorCode.INTERNAL_APPLICATION_ERROR, message);
    }

    public InternalApplicationException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }

    public InternalApplicationException(final ErrorDefinition errorDefinition, final String technicalMessage) {
        super(errorDefinition, technicalMessage);
    }

    public InternalApplicationException(final String message, final Throwable cause) {
        super(CommonErrorCode.INTERNAL_APPLICATION_ERROR, message, cause);
    }

    public InternalApplicationException(final String code, final String message) {
        super(code, message);
    }

    /**
     * VAL H3 (REA-02/REA-06): una dependencia externa obligatoria no esta disponible (caida, timeout o circuit breaker abierto).
     *
     * <p>Sigue siendo un fallo interno (fail-closed, sin efectos), pero se publica como 503 con el codigo de su definicion para que
     * clientes y alertas distingan "dependencia caida, reintente" de un error del backend (500): {@code ApiErrorCatalog} toma el
     * estado del tipo de la definicion, que debe ser {@link ErrorKind#SERVICE_UNAVAILABLE}. Es una fabrica y no una subclase para
     * no superar la profundidad de herencia permitida (Sonar java:S110).</p>
     */
    public static InternalApplicationException servicioNoDisponible(final ErrorDefinition errorDefinition, final Throwable cause) {
        Objects.requireNonNull(errorDefinition, "La definicion de error es obligatoria.");
        if (errorDefinition.kind() != ErrorKind.SERVICE_UNAVAILABLE) {
            throw new IllegalArgumentException("La definicion " + errorDefinition.code() + " no es de tipo SERVICE_UNAVAILABLE.");
        }
        return new InternalApplicationException(errorDefinition, errorDefinition.defaultMessage(), cause);
    }

    protected InternalApplicationException(final ErrorDefinition errorDefinition, final String message, final Throwable cause) {
        super(errorDefinition, message, cause);
    }

    protected InternalApplicationException(final String code, final String message, final Throwable cause) {
        super(code, message, cause);
    }
}
