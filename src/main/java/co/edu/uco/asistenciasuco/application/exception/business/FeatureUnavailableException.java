package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;

public final class FeatureUnavailableException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public FeatureUnavailableException(final String message) {
        super("FEATURE_UNAVAILABLE", message);
    }

    /** Funcionalidad no disponible con texto de catalogo (VAL-oleada3 MSG-05); responde 501 como la variante de texto. */
    public FeatureUnavailableException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }
}
