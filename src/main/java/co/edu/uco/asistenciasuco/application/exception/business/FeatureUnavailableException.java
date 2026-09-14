package co.edu.uco.asistenciasuco.application.exception.business;

public final class FeatureUnavailableException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public FeatureUnavailableException(final String message) {
        super("FEATURE_UNAVAILABLE", message);
    }
}
