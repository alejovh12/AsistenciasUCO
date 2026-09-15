package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Validador neutro y reutilizable: exige que un claim configurable exista, tenga texto y sea
 * un UUID válido. No conoce el nombre {@code idUsuario} de forma interna — el nombre del claim
 * se recibe por configuración, para que pueda reutilizarse con cualquier claim UUID requerido.
 */
public final class RequiredUuidClaimValidator implements OAuth2TokenValidator<Jwt> {

    private final String claimName;

    public RequiredUuidClaimValidator(final String claimName) {
        if (claimName == null || claimName.isBlank()) {
            throw new IllegalArgumentException("claimName es obligatorio.");
        }
        this.claimName = claimName;
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt token) {
        final Object rawValue = token.getClaim(claimName);
        if (!(rawValue instanceof String value) || value.isBlank()) {
            return failure();
        }
        try {
            UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return failure();
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "El claim '" + claimName + "' es obligatorio y debe tener formato UUID.",
                null
        ));
    }
}
