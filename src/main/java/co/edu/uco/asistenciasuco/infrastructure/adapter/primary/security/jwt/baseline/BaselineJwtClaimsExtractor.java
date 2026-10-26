package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduce la identidad central verificada del baseline a la UUID institucional. No usa el
 * {@code sub} como UUID ni concede acceso cuando el correo no está verificado, no existe, está
 * inactivo o es ambiguo.
 */
public final class BaselineJwtClaimsExtractor implements JwtClaimsExtractor {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final Set<String> VALID_ROLES = Arrays.stream(InstitutionalRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final String apiClientId;
    private final String emailClaim;
    private final String emailVerifiedClaim;
    private final InstitutionalScopePort institutionalScope;

    public BaselineJwtClaimsExtractor(
            final String apiClientId,
            final String emailClaim,
            final String emailVerifiedClaim,
            final InstitutionalScopePort institutionalScope
    ) {
        this.apiClientId = Objects.requireNonNull(apiClientId, "apiClientId es obligatorio.");
        this.emailClaim = Objects.requireNonNull(emailClaim, "emailClaim es obligatorio.");
        this.emailVerifiedClaim = Objects.requireNonNull(emailVerifiedClaim, "emailVerifiedClaim es obligatorio.");
        this.institutionalScope = Objects.requireNonNull(institutionalScope, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public java.util.UUID requireUserId(final Jwt jwt) {
        requireSubject(jwt);
        final Object verified = jwt.getClaim(emailVerifiedClaim);
        if (!Boolean.TRUE.equals(verified)) {
            throw invalidToken("El token baseline no contiene evidencia de correo verificado.");
        }
        final Object rawEmail = jwt.getClaim(emailClaim);
        if (!(rawEmail instanceof String email) || email.isBlank()) {
            throw invalidToken("El token baseline no contiene un correo institucional.");
        }
        try {
            return institutionalScope.findUsuarioIdByEmail(normalizeEmail(email))
                    .filter(institutionalScope::isUsuarioActivo)
                    .orElseThrow(() -> invalidToken("La identidad institucional no existe o está inactiva."));
        } catch (InstitutionalIdentityAmbiguityException exception) {
            throw invalidToken("La identidad institucional es ambigua.");
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

    private static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireSubject(final Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw invalidToken("El token baseline no contiene subject.");
        }
    }

    private static OAuth2AuthenticationException invalidToken(final String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
