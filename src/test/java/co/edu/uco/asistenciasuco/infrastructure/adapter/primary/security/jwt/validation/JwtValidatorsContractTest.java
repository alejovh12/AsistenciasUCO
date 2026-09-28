package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato de los validators JWT institucionales que no cubren sus tests de decision:
 * validacion del constructor, emisor ausente, alias de emisor y semantica de igualdad
 * (necesaria para que dos decoders configurados igual sean intercambiables).
 */
class JwtValidatorsContractTest {

    private static Jwt.Builder baseJwt() {
        final Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("sub")
                .issuedAt(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300));
    }

    // ------------------------------------------------------------ InstitutionalIssuerValidator

    @Test
    void issuer_token_sin_claim_iss_falla_con_invalid_token() {
        final InstitutionalIssuerValidator validator =
                new InstitutionalIssuerValidator("https://issuer.uco.edu.co/realms/uco");

        final OAuth2TokenValidatorResult result = validator.validate(baseJwt().claim("otro", "x").build());

        assertTrue(result.hasErrors());
        final OAuth2Error error = result.getErrors().iterator().next();
        assertEquals(OAuth2ErrorCodes.INVALID_TOKEN, error.getErrorCode());
        assertTrue(error.getDescription().contains("https://issuer.uco.edu.co/realms/uco"));
    }

    @Test
    void issuer_normaliza_espacios_y_solo_acepta_el_emisor_configurado_cuando_no_es_loopback() {
        final InstitutionalIssuerValidator validator =
                new InstitutionalIssuerValidator("  https://issuer.uco.edu.co/realms/uco  ");

        assertEquals("https://issuer.uco.edu.co/realms/uco", validator.expectedIssuer());
        assertEquals(Set.of("https://issuer.uco.edu.co/realms/uco"), validator.acceptedIssuers());
        assertFalse(validator.validate(baseJwt().issuer("https://issuer.uco.edu.co/realms/uco").build()).hasErrors());
        assertTrue(validator.validate(baseJwt().issuer("https://otro.uco.edu.co/realms/uco").build()).hasErrors());
    }

    @Test
    void issuer_loopback_acepta_ambos_alias_y_los_expone_en_orden_estable() {
        final InstitutionalIssuerValidator localhost =
                new InstitutionalIssuerValidator("http://localhost:8081/realms/uco");
        final InstitutionalIssuerValidator loopback =
                new InstitutionalIssuerValidator("http://127.0.0.1:8081/realms/uco");

        assertEquals(List.of("http://localhost:8081/realms/uco", "http://127.0.0.1:8081/realms/uco"),
                List.copyOf(localhost.acceptedIssuers()));
        assertEquals(List.of("http://127.0.0.1:8081/realms/uco", "http://localhost:8081/realms/uco"),
                List.copyOf(loopback.acceptedIssuers()));
        assertThrows(UnsupportedOperationException.class, () -> localhost.acceptedIssuers().add("http://evil"));
    }

    @Test
    void issuer_igualdad_depende_solo_del_emisor_esperado() {
        final InstitutionalIssuerValidator a = new InstitutionalIssuerValidator("https://issuer.uco.edu.co/realms/uco");
        final InstitutionalIssuerValidator sameIssuer = new InstitutionalIssuerValidator(" https://issuer.uco.edu.co/realms/uco ");
        final InstitutionalIssuerValidator otherIssuer = new InstitutionalIssuerValidator("https://issuer.uco.edu.co/realms/otro");

        assertEquals(a, a);
        assertEquals(a, sameIssuer);
        assertEquals(a.hashCode(), sameIssuer.hashCode());
        assertNotEquals(a, otherIssuer);
        assertNotEquals(a, "https://issuer.uco.edu.co/realms/uco");
        assertNotEquals(a, null);
    }

    // ------------------------------------------------------------ AudienceValidator

    @Test
    void audience_rechaza_audiencia_esperada_nula_o_en_blanco() {
        assertThrows(IllegalArgumentException.class, () -> new AudienceValidator(null));
        assertThrows(IllegalArgumentException.class, () -> new AudienceValidator("  "));
    }

    @Test
    void audience_igualdad_depende_solo_de_la_audiencia_esperada() {
        final AudienceValidator api = new AudienceValidator("asistencias-api");

        assertEquals(api, api);
        assertEquals(api, new AudienceValidator("asistencias-api"));
        assertEquals(api.hashCode(), new AudienceValidator("asistencias-api").hashCode());
        assertNotEquals(api, new AudienceValidator("otra-api"));
        assertNotEquals(api, "asistencias-api");
        assertNotEquals(api, null);
    }

    // ------------------------------------------------------------ RequiredUuidClaimValidator

    @Test
    void uuid_claim_rechaza_nombre_de_claim_nulo_o_en_blanco() {
        assertThrows(IllegalArgumentException.class, () -> new RequiredUuidClaimValidator(null));
        assertThrows(IllegalArgumentException.class, () -> new RequiredUuidClaimValidator(" "));
    }

    @Test
    void uuid_claim_acepta_uuid_valido_con_espacios_y_rechaza_valores_no_uuid() {
        final RequiredUuidClaimValidator validator = new RequiredUuidClaimValidator("idUsuario");

        assertFalse(validator.validate(baseJwt()
                .claim("idUsuario", " c0a80101-0000-0000-0000-000000000001 ").build()).hasErrors());
        assertTrue(validator.validate(baseJwt().claim("idUsuario", "no-es-uuid").build()).hasErrors());
        assertTrue(validator.validate(baseJwt().claim("idUsuario", "  ").build()).hasErrors());
        assertTrue(validator.validate(baseJwt().claim("idUsuario", 42).build()).hasErrors());
        assertTrue(validator.validate(baseJwt().claim("otro", "c0a80101-0000-0000-0000-000000000001").build()).hasErrors());
    }
}
