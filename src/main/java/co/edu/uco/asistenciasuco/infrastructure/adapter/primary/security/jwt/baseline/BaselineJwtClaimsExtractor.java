package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduce la identidad central verificada del baseline a la UUID institucional mediante el vínculo persistente
 * {@code issuer + subject → Usuario.id} (ver {@link ExternalIdentityResolver}). No usa el {@code sub} como UUID, no
 * trata el correo como identificador inmutable y no concede acceso cuando la identidad no existe, está revocada, el
 * usuario está inactivo, hay conflicto o ambigüedad.
 */
public final class BaselineJwtClaimsExtractor implements JwtClaimsExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineJwtClaimsExtractor.class);
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final Set<String> VALID_ROLES = Arrays.stream(InstitutionalRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final String apiClientId;
    private final String emailClaim;
    private final String emailVerifiedClaim;
    private final String userIdClaim;
    private final ExternalIdentityResolver identityResolver;

    public BaselineJwtClaimsExtractor(
            final String apiClientId,
            final String emailClaim,
            final String emailVerifiedClaim,
            final String userIdClaim,
            final ExternalIdentityResolver identityResolver
    ) {
        this.apiClientId = Objects.requireNonNull(apiClientId, "apiClientId es obligatorio.");
        this.emailClaim = Objects.requireNonNull(emailClaim, "emailClaim es obligatorio.");
        this.emailVerifiedClaim = Objects.requireNonNull(emailVerifiedClaim, "emailVerifiedClaim es obligatorio.");
        this.userIdClaim = Objects.requireNonNull(userIdClaim, "userIdClaim es obligatorio.");
        this.identityResolver = Objects.requireNonNull(identityResolver, "ExternalIdentityResolver es obligatorio.");
    }

    @Override
    public java.util.UUID requireUserId(final Jwt jwt) {
        final Object rawEmail = jwt.getClaim(emailClaim);
        final String email = rawEmail instanceof String text ? text : null;
        final boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim(emailVerifiedClaim));
        final String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        try {
            final ExternalIdentityResolver.Resolution resolution =
                    identityResolver.resolve(issuer, jwt.getSubject(), email, emailVerified);
            requireConsistentUserIdClaim(jwt, resolution.usuarioId(), issuer);
            if (resolution.newlyLinked()) {
                // Trazabilidad del alta del vínculo: identificadores opacos, nunca tokens ni correos.
                LOGGER.info("Identidad externa vinculada. usuarioId={}, subject={}, issuer={}",
                        resolution.usuarioId(), jwt.getSubject(), issuer);
            }
            return resolution.usuarioId();
        } catch (ExternalIdentityRejectedException exception) {
            LOGGER.warn("Identidad externa rechazada. reason={}, subject={}, issuer={}",
                    exception.reason(), jwt.getSubject(), issuer);
            throw invalidToken("La identidad central no está autorizada para AsistenciasUCO.");
        }
    }

    /**
     * El claim {@code idUsuario} (atributo de la cuenta en el IdP, usado por la UI) nunca es autoridad: si viene en
     * el token debe coincidir con el usuario resuelto por el vínculo persistente; una divergencia indica una cuenta
     * mal aprovisionada o un intento de suplantación y se rechaza.
     */
    private void requireConsistentUserIdClaim(final Jwt jwt, final java.util.UUID resolved, final String issuer) {
        final Object claim = jwt.getClaim(userIdClaim);
        if (claim == null) {
            return;
        }
        final boolean consistent = claim instanceof String text && parseUuid(text).filter(resolved::equals).isPresent();
        if (!consistent) {
            LOGGER.warn("Identidad externa rechazada. reason=USER_ID_CLAIM_MISMATCH, subject={}, issuer={}",
                    jwt.getSubject(), issuer);
            throw invalidToken("La identidad central no está autorizada para AsistenciasUCO.");
        }
    }

    private static java.util.Optional<java.util.UUID> parseUuid(final String text) {
        try {
            return java.util.Optional.of(java.util.UUID.fromString(text.trim()));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    @Override
    public Set<InstitutionalRole> extractRoles(final Jwt jwt) {
        final Object resourceAccessRaw = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (!(resourceAccessRaw instanceof Map<?, ?> resourceAccess)) {
            return Set.of();
        }
        final Object clientEntryRaw = resourceAccess.get(apiClientId);
        if (!(clientEntryRaw instanceof Map<?, ?> clientEntry)) {
            return Set.of();
        }
        final Object rolesRaw = clientEntry.get(ROLES_CLAIM);
        if (!(rolesRaw instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(VALID_ROLES::contains)
                .map(InstitutionalRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static OAuth2AuthenticationException invalidToken(final String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
