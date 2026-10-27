package co.edu.uco.asistenciasuco.application.secondaryports.security;

/** La identidad o la cuenta ya están ocupadas por otro vínculo; nunca se resuelve en silencio. */
public final class ExternalIdentityConflictException extends RuntimeException {

    public ExternalIdentityConflictException(final String message) {
        super(message);
    }
}
