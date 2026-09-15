package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Única clase de todo el runtime de seguridad que conoce cómo Keycloak estructura sus
 * claims: el claim institucional {@code idUsuario} y los roles de cliente bajo
 * {@code resource_access[apiClientId].roles}.
 *
 * <p>Deliberadamente NO lee {@code realm_access} (ver estándar: solo roles del client de la
 * API cuentan para autorización) y hace parsing defensivo — un token mal formado nunca debe
 * producir {@link ClassCastException} ni un 500.</p>
 *
 * <p>El mapping token→rol es EXACTO y controlado: solo se aceptan los nombres institucionales
 * literales ({@code ADMINISTRADOR}, {@code DECANO}, {@code COORDINADOR}, {@code DOCENTE},
 * {@code ESTUDIANTE}). No se aceptan códigos cortos, alias, prefijos {@code ROLE_} ni
 * variantes de mayúsculas/minúsculas — cualquier otro string se ignora en silencio, nunca
 * lanza error.</p>
 */
public final class KeycloakJwtClaimsExtractor implements JwtClaimsExtractor {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    private static final Set<String> VALID_INSTITUTIONAL_ROLE_NAMES = Arrays.stream(InstitutionalRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final String apiClientId;
    private final String userIdClaim;

    public KeycloakJwtClaimsExtractor(final String apiClientId, final String userIdClaim) {
        this.apiClientId = Objects.requireNonNull(apiClientId, "apiClientId es obligatorio.");
        this.userIdClaim = Objects.requireNonNull(userIdClaim, "userIdClaim es obligatorio.");
    }

    @Override
    public UUID requireUserId(final Jwt jwt) {
        final Object rawValue = jwt.getClaim(userIdClaim);
        if (!(rawValue instanceof String value) || value.isBlank()) {
            throw invalidToken("El token no contiene un claim '" + userIdClaim + "' valido.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw invalidToken("El claim '" + userIdClaim + "' no tiene formato UUID valido.");
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
                .filter(role -> role instanceof String)
                .map(role -> (String) role)
                .map(KeycloakJwtClaimsExtractor::toInstitutionalRole)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Mapping exacto y controlado: solo acepta el nombre institucional literal. Ningún código
     * corto ("DO", "ES"...), ningún alias con prefijo {@code ROLE_}, ninguna variante de caso.
     * Un rol ajeno o desconocido se ignora devolviendo {@link Optional#empty()} — nunca lanza.
     */
    private static Optional<InstitutionalRole> toInstitutionalRole(final String rawRole) {
        if (rawRole == null || !VALID_INSTITUTIONAL_ROLE_NAMES.contains(rawRole)) {
            return Optional.empty();
        }
        return Optional.of(InstitutionalRole.valueOf(rawRole));
    }

    private static OAuth2AuthenticationException invalidToken(final String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
