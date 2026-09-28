package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstitutionalIssuerValidatorTest {

    private static final String CONFIGURED_ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private final InstitutionalIssuerValidator validator = new InstitutionalIssuerValidator(CONFIGURED_ISSUER);

    @Test
    void emisor_exacto_pasa_validacion() {
        final Jwt jwt = buildJwt("http://127.0.0.1:8081/realms/asistencias-uco");
        assertTrue(validator.validate(jwt).getErrors().isEmpty());
    }

    @Test
    void emisor_alias_localhost_pasa_validacion_cuando_se_configura_127_0_0_1() {
        final Jwt jwt = buildJwt("http://localhost:8081/realms/asistencias-uco");
        assertTrue(validator.validate(jwt).getErrors().isEmpty());
    }

    @Test
    void emisor_alias_127_0_0_1_pasa_validacion_cuando_se_configura_localhost() {
        final InstitutionalIssuerValidator localhostValidator =
                new InstitutionalIssuerValidator("http://localhost:8081/realms/asistencias-uco");
        final Jwt jwt = buildJwt("http://127.0.0.1:8081/realms/asistencias-uco");
        assertTrue(localhostValidator.validate(jwt).getErrors().isEmpty());
    }

    @Test
    void emisor_distinto_falla_validacion() {
        final Jwt jwt = buildJwt("http://malicious-issuer.com/realms/asistencias-uco");
        assertFalse(validator.validate(jwt).getErrors().isEmpty());
    }

    @Test
    void emisor_nulo_o_vacio_en_constructor_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalIssuerValidator(null));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalIssuerValidator("   "));
    }

    private static Jwt buildJwt(final String issuer) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(issuer)
                .subject("sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("asistencias-api"))
                .claim("idUsuario", "c0a80101-0000-0000-0000-000000000001")
                .build();
    }
}
