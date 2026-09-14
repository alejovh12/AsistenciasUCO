package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.validation;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudienceValidatorTest {

    private static final String EXPECTED_AUDIENCE = "asistencias-api";

    private final AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);

    @Test
    void expected_audience_presente_pasa_validacion() {
        assertTrue(validator.validate(jwtWithAudience(List.of(EXPECTED_AUDIENCE))).getErrors().isEmpty());
    }

    @Test
    void audience_ausente_falla_validacion() {
        assertFalse(validator.validate(jwtWithAudience(null)).getErrors().isEmpty());
    }

    @Test
    void audience_vacia_falla_validacion() {
        assertFalse(validator.validate(jwtWithAudience(List.of())).getErrors().isEmpty());
    }

    @Test
    void audience_incorrecta_falla_validacion() {
        assertFalse(validator.validate(jwtWithAudience(List.of("otra-api"))).getErrors().isEmpty());
    }

    @Test
    void multiples_audiences_incluyendo_la_correcta_pasa_validacion() {
        assertTrue(validator.validate(jwtWithAudience(List.of("account", EXPECTED_AUDIENCE, "otra-api")))
                .getErrors().isEmpty());
    }

    private static Jwt jwtWithAudience(final List<String> audience) {
        final Instant now = Instant.now();
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                .subject("sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300));
        if (audience != null) {
            builder.audience(audience);
        }
        return builder.build();
    }
}
