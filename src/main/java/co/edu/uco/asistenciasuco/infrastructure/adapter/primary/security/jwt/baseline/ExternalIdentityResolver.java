package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline.ExternalIdentityRejectedException.Reason;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Resuelve una identidad OIDC ya verificada por el {@code JwtDecoder} a {@code Usuario.id}.
 *
 * <ol>
 *   <li>Si {@code issuer + subject} ya está vinculado y vigente, devuelve ese usuario (siempre que siga activo) sin
 *       mirar el correo: un cambio de correo en el IdP no reasigna la cuenta.</li>
 *   <li>Si no hay vínculo, exige correo verificado, lo normaliza, localiza una coincidencia única y activa en SQL
 *       Server y registra el vínculo de forma atómica.</li>
 *   <li>Un vínculo revocado, un conflicto o cualquier ambigüedad rechazan; nunca se elige una fila al azar.</li>
 * </ol>
 */
public final class ExternalIdentityResolver {

    /** Resultado de la resolución; {@code newlyLinked} permite trazar el alta del vínculo. */
    public record Resolution(UUID usuarioId, boolean newlyLinked) {
    }

    private final InstitutionalScopePort institutionalScope;
    private final ExternalIdentityLinkPort links;

    public ExternalIdentityResolver(
            final InstitutionalScopePort institutionalScope,
            final ExternalIdentityLinkPort links
    ) {
        this.institutionalScope = Objects.requireNonNull(institutionalScope, "InstitutionalScopePort es obligatorio.");
        this.links = Objects.requireNonNull(links, "ExternalIdentityLinkPort es obligatorio.");
    }

    public Resolution resolve(
            final String issuer,
            final String subject,
            final String rawEmail,
            final boolean emailVerified
    ) {
        if (issuer == null || issuer.isBlank()) {
            throw new ExternalIdentityRejectedException(Reason.MISSING_ISSUER);
        }
        if (subject == null || subject.isBlank()) {
            throw new ExternalIdentityRejectedException(Reason.MISSING_SUBJECT);
        }
        final var existing = links.find(issuer, subject);
        if (existing.isPresent()) {
            return resolveExisting(existing.get());
        }
        return linkFirstTime(issuer, subject, rawEmail, emailVerified);
    }

    private Resolution resolveExisting(final ExternalIdentityLinkPort.ExternalIdentityLink link) {
        if (!link.vigente()) {
            throw new ExternalIdentityRejectedException(Reason.LINK_REVOKED);
        }
        if (!institutionalScope.isUsuarioActivo(link.usuarioId())) {
            throw new ExternalIdentityRejectedException(Reason.USER_INACTIVE);
        }
        return new Resolution(link.usuarioId(), false);
    }

    private Resolution linkFirstTime(
            final String issuer,
            final String subject,
            final String rawEmail,
            final boolean emailVerified
    ) {
        if (!emailVerified) {
            throw new ExternalIdentityRejectedException(Reason.EMAIL_NOT_VERIFIED);
        }
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ExternalIdentityRejectedException(Reason.EMAIL_MISSING);
        }
        final String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        final UUID usuarioId;
        try {
            usuarioId = institutionalScope.findUsuarioIdByEmail(email)
                    .orElseThrow(() -> new ExternalIdentityRejectedException(Reason.USER_UNKNOWN));
        } catch (InstitutionalIdentityAmbiguityException _) {
            throw new ExternalIdentityRejectedException(Reason.USER_AMBIGUOUS);
        }
        if (!institutionalScope.isUsuarioActivo(usuarioId)) {
            throw new ExternalIdentityRejectedException(Reason.USER_INACTIVE);
        }
        try {
            return new Resolution(usuarioId, links.link(issuer, subject, usuarioId, email));
        } catch (ExternalIdentityConflictException _) {
            throw new ExternalIdentityRejectedException(Reason.LINK_CONFLICT);
        }
    }
}
