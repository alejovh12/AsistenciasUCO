package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Vínculo persistente entre una identidad OIDC externa ({@code issuer + subject}) y el
 * {@code Usuario.id} institucional. El correo solo participa en la vinculación inicial; después el
 * vínculo se resuelve por {@code issuer + subject}, de modo que un cambio de correo en el IdP no
 * reasigna ni apropia cuentas.
 */
public interface ExternalIdentityLinkPort {

    /** Vínculo registrado para la identidad externa, vigente o revocado. */
    Optional<ExternalIdentityLink> find(String issuer, String subject);

    /**
     * Registra el vínculo de forma atómica. Es idempotente para el mismo par identidad/usuario vigente.
     *
     * @return {@code true} si se creó el vínculo; {@code false} si ya existía idéntico y vigente.
     * @throws ExternalIdentityConflictException si la identidad ya ocupa otra cuenta o fue revocada, o si la cuenta
     *                                           ya tiene otra identidad vigente del mismo emisor.
     */
    boolean link(String issuer, String subject, UUID usuarioId, String verifiedEmail);

    /** Revoca el vínculo conservando la fila; idempotente. Una identidad revocada no se reutiliza. */
    void revoke(String issuer, String subject);

    record ExternalIdentityLink(UUID usuarioId, boolean vigente) {
    }

    /** La identidad o la cuenta ya están ocupadas por otro vínculo; nunca se resuelve en silencio. */
    final class ExternalIdentityConflictException extends RuntimeException {

        public ExternalIdentityConflictException(final String message) {
            super(message);
        }
    }
}
