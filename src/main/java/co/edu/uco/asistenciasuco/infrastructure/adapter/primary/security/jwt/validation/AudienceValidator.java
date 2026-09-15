package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Objects;

/**
 * Validador neutro (no conoce ningún proveedor OIDC) que exige que {@code aud} contenga
 * exactamente la audiencia esperada. Un token cuya audiencia sea, por ejemplo, únicamente
 * {@code account} o un client de frontend no es aceptado como sustituto de la audiencia
 * de la API.
 */
public final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(final String expectedAudience) {
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalArgumentException("expectedAudience es obligatorio.");
        }
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt token) {
        final List<String> audience = token.getAudience();
        if (audience == null || audience.isEmpty() || !audience.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "El token no contiene la audiencia esperada.",
                    null
            ));
        }
        return OAuth2TokenValidatorResult.success();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof AudienceValidator otherValidator
                && Objects.equals(expectedAudience, otherValidator.expectedAudience);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectedAudience);
    }
}
