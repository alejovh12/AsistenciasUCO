package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.keycloak;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.KeycloakSecurityProviderProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica la composicion real de validators del {@code JwtDecoder} de Keycloak: un token firmado
 * solo es aceptado si cumple emisor institucional, audiencia de la API, vigencia y claim de usuario UUID.
 * La resolucion del emisor por red se sustituye por una clave publica local (no se usa Keycloak real).
 */
class KeycloakJwtDecoderCompositionTest {

    private static final String ISSUER = "https://issuer.uco.edu.co/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String USER_ID = "c0a80101-0000-0000-0000-000000000001";

    private static KeyPair keyPair;

    private final KeycloakSecurityProviderProperties properties =
            new KeycloakSecurityProviderProperties(ISSUER, "asistencias-api", AUDIENCE, USER_ID_CLAIM);
    private final KeycloakSecurityAdapterConfiguration configuration = new KeycloakSecurityAdapterConfiguration();

    @BeforeAll
    static void generateKeys() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    private JwtDecoder buildDecoder() {
        final NimbusJwtDecoder delegate = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        try (MockedStatic<JwtDecoders> decoders = Mockito.mockStatic(JwtDecoders.class)) {
            decoders.when(() -> JwtDecoders.fromIssuerLocation(ISSUER)).thenReturn(delegate);
            return configuration.jwtDecoder(properties);
        }
    }

    private static JWTClaimsSet.Builder validClaims() {
        final Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject("subject-1")
                .audience(AUDIENCE)
                .claim(USER_ID_CLAIM, USER_ID)
                .issueTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(now.plusSeconds(300)));
    }

    private static String sign(final JWTClaimsSet claims) throws Exception {
        final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }

    @Test
    void jwtClaimsExtractor_se_construye_con_el_cliente_y_el_claim_configurados() {
        final JwtClaimsExtractor extractor = configuration.jwtClaimsExtractor(properties);

        assertInstanceOf(KeycloakJwtClaimsExtractor.class, extractor);
    }

    @Test
    void acepta_token_con_emisor_audiencia_vigencia_y_usuario_validos() throws Exception {
        final Jwt jwt = buildDecoder().decode(sign(validClaims().build()));

        assertEquals(USER_ID, jwt.getClaimAsString(USER_ID_CLAIM));
        assertEquals(ISSUER, jwt.getIssuer().toString());
    }

    @Test
    void rechaza_token_de_otro_emisor() throws Exception {
        final String token = sign(validClaims().issuer("https://evil.example.com/realms/otro").build());

        assertThrows(BadJwtException.class, () -> buildDecoder().decode(token));
    }

    @Test
    void rechaza_token_sin_la_audiencia_de_la_api() throws Exception {
        final String token = sign(validClaims().audience("account").build());

        assertThrows(BadJwtException.class, () -> buildDecoder().decode(token));
    }

    @Test
    void rechaza_token_sin_claim_de_usuario_uuid() throws Exception {
        final String sinClaim = sign(validClaims().claim(USER_ID_CLAIM, null).build());
        final String claimInvalido = sign(validClaims().claim(USER_ID_CLAIM, "no-es-uuid").build());
        final JwtDecoder decoder = buildDecoder();

        assertThrows(BadJwtException.class, () -> decoder.decode(sinClaim));
        assertThrows(BadJwtException.class, () -> decoder.decode(claimInvalido));
    }

    @Test
    void rechaza_token_expirado() throws Exception {
        final Instant now = Instant.now();
        final String token = sign(validClaims()
                .issueTime(Date.from(now.minusSeconds(1200)))
                .expirationTime(Date.from(now.minusSeconds(600)))
                .build());

        assertThrows(BadJwtException.class, () -> buildDecoder().decode(token));
    }

    @Test
    void rechaza_token_firmado_con_otra_clave() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final KeyPair attacker = generator.generateKeyPair();
        final SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), validClaims().build());
        forged.sign(new RSASSASigner((RSAPrivateKey) attacker.getPrivate()));
        final String token = forged.serialize();

        assertThrows(BadJwtException.class, () -> buildDecoder().decode(token));
    }
}
