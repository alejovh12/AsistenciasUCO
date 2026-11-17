package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.baseline;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineProviderProperties;
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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PRU-17: composicion real del {@code JwtDecoder} del modo baseline (proveedor por defecto). Un token firmado solo se
 * acepta con firma valida de la clave del emisor, vigencia ({@code exp} y {@code nbf}), emisor institucional y
 * audiencia de la API. Cualquier rechazo es {@link BadJwtException}, que la cadena de Spring Security traduce a 401
 * (cubierto en {@code SecurityConfigTest}). La resolucion del emisor por red se sustituye por una clave publica local.
 */
class BaselineJwtDecoderCompositionTest {

    private static final String ISSUER = "https://security-baseline.uco.edu.co/realms/uco";
    private static final String AUDIENCE = "asistencias-api";

    private static KeyPair keyPair;

    private final SecurityBaselineProviderProperties properties = new SecurityBaselineProviderProperties(
            ISSUER, "asistencias-api", AUDIENCE, "email", "email_verified", "sub", "");
    private final BaselineSecurityAdapterConfiguration configuration = new BaselineSecurityAdapterConfiguration();

    @BeforeAll
    static void generarClaves() throws Exception {
        keyPair = nuevoParDeClaves();
    }

    private static KeyPair nuevoParDeClaves() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private JwtDecoder decoder() {
        final NimbusJwtDecoder delegate = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        try (MockedStatic<JwtDecoders> decoders = Mockito.mockStatic(JwtDecoders.class)) {
            decoders.when(() -> JwtDecoders.fromIssuerLocation(ISSUER)).thenReturn(delegate);
            return configuration.jwtDecoder(properties);
        }
    }

    private static JWTClaimsSet.Builder claimsValidos() {
        final Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject("c0a80101-0000-0000-0000-000000000001")
                .audience(AUDIENCE)
                .issueTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(now.plusSeconds(300)));
    }

    private static String firmar(final JWTClaimsSet claims, final KeyPair claves) throws Exception {
        final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) claves.getPrivate()));
        return jwt.serialize();
    }

    @Test
    void acepta_token_con_firma_vigencia_emisor_y_audiencia_validos() throws Exception {
        final Jwt jwt = decoder().decode(firmar(claimsValidos().build(), keyPair));

        assertEquals(ISSUER, jwt.getIssuer().toString());
        assertEquals("c0a80101-0000-0000-0000-000000000001", jwt.getSubject());
    }

    @Test
    void rechaza_token_expirado_fuera_de_la_tolerancia_de_reloj() throws Exception {
        final Instant now = Instant.now();
        final String token = firmar(claimsValidos()
                .issueTime(Date.from(now.minusSeconds(1200)))
                .expirationTime(Date.from(now.minusSeconds(600)))
                .build(), keyPair);

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_cuyo_nbf_aun_no_llega() throws Exception {
        final Instant now = Instant.now();
        final String token = firmar(claimsValidos()
                .notBeforeTime(Date.from(now.plusSeconds(600)))
                .expirationTime(Date.from(now.plusSeconds(1200)))
                .build(), keyPair);

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_con_carga_alterada_despues_de_firmar() throws Exception {
        final String[] partes = firmar(claimsValidos().build(), keyPair).split("\\.");
        final String cargaAlterada = Base64.getUrlEncoder().withoutPadding().encodeToString(
                claimsValidos().subject("c0a80101-0000-0000-0000-0000000000ff").build().toString()
                        .getBytes(StandardCharsets.UTF_8));
        final String token = partes[0] + "." + cargaAlterada + "." + partes[2];

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_con_firma_truncada() throws Exception {
        final String original = firmar(claimsValidos().build(), keyPair);
        final String token = original.substring(0, original.length() - 8);

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_firmado_con_otra_clave() throws Exception {
        final String token = firmar(claimsValidos().build(), nuevoParDeClaves());

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_sin_firma_alg_none() {
        final String cabecera = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        final String carga = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claimsValidos().build().toString().getBytes(StandardCharsets.UTF_8));
        final String token = cabecera + "." + carga + ".";

        final var decoder2 = decoder();
        assertThrows(BadJwtException.class, () -> decoder2.decode(token));
    }

    @Test
    void rechaza_token_de_otro_emisor_o_sin_la_audiencia_de_la_api() throws Exception {
        final String otroEmisor = firmar(claimsValidos().issuer("https://evil.example.com/realms/uco").build(), keyPair);
        final String otraAudiencia = firmar(claimsValidos().audience("account").build(), keyPair);
        final JwtDecoder decoder = decoder();

        assertThrows(BadJwtException.class, () -> decoder.decode(otroEmisor));
        assertThrows(BadJwtException.class, () -> decoder.decode(otraAudiencia));
    }
}
