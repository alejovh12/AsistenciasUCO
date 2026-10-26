package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

/** Rechazo de una identidad externa; el motivo es para trazabilidad y nunca incluye tokens ni correos. */
public final class ExternalIdentityRejectedException extends RuntimeException {

    public enum Reason {
        MISSING_ISSUER,
        MISSING_SUBJECT,
        EMAIL_NOT_VERIFIED,
        EMAIL_MISSING,
        LINK_REVOKED,
        USER_UNKNOWN,
        USER_INACTIVE,
        USER_AMBIGUOUS,
        LINK_CONFLICT
    }

    private final Reason reason;

    public ExternalIdentityRejectedException(final Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
