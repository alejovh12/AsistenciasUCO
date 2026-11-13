package co.edu.uco.asistenciasuco.application.exception.internal;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

import java.util.Objects;

/**
 * VAL H3 (REA-02/REA-06): una dependencia externa obligatoria no esta disponible (caida, timeout o circuit breaker abierto).
 *
 * <p>Sigue siendo un fallo interno (fail-closed, sin efectos), pero se publica como 503 con el codigo de su definicion para que
 * clientes y alertas distingan "dependencia caida, reintente" de un error del backend (500). La definicion debe ser de tipo
 * {@link ErrorKind#SERVICE_UNAVAILABLE}.</p>
 */
public class ServiceUnavailableException extends InternalApplicationException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(final ErrorDefinition errorDefinition, final Throwable cause) {
        super(requireServiceUnavailable(errorDefinition), errorDefinition.defaultMessage(), cause);
    }

    private static ErrorDefinition requireServiceUnavailable(final ErrorDefinition errorDefinition) {
        Objects.requireNonNull(errorDefinition, "La definicion de error es obligatoria.");
        if (errorDefinition.kind() != ErrorKind.SERVICE_UNAVAILABLE) {
            throw new IllegalArgumentException("La definicion " + errorDefinition.code() + " no es de tipo SERVICE_UNAVAILABLE.");
        }
        return errorDefinition;
    }
}
