package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.validation;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredUuidClaimValidatorTest {

    private static final String CLAIM_NAME = "idUsuario";
    private static final String VALID_UUID = "93641bab-e3cd-485c-b275-47e7b731e18c";

    private final RequiredUuidClaimValidator validator = new RequiredUuidClaimValidator(CLAIM_NAME);

    @Test
    void claim_valido_pasa_validacion() {
        assertTrue(validator.validate(jwtWithClaims(Map.of(CLAIM_NAME, VALID_UUID))).getErrors().isEmpty());
    }

    @Test
    void claim_ausente_falla_validacion() {
        assertFalse(validator.validate(jwtWithClaims(Map.of())).getErrors().isEmpty());
    }

    @Test
    void claim_null_falla_validacion() {
        final Instant now = Instant.now();
        final Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_NAME, null);
        final Jwt jwt = new Jwt(
                "token",
                now.minusSeconds(60),
                now.plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );

        assertFalse(validator.validate(jwt).getErrors().isEmpty());
    }

    @Test
    void claim_blank_falla_validacion() {
        assertFalse(validator.validate(jwtWithClaims(Map.of(CLAIM_NAME, "   "))).getErrors().isEmpty());
    }

    @Test
    void claim_invalido_falla_validacion() {
        assertFalse(validator.validate(jwtWithClaims(Map.of(CLAIM_NAME, "no-es-un-uuid"))).getErrors().isEmpty());
    }

    private static Jwt jwtWithClaims(final Map<String, Object> claims) {
        final Instant now = Instant.now();
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                .subject("sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
