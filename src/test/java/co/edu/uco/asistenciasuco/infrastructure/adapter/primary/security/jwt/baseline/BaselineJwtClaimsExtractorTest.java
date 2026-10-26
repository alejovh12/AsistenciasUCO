package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaselineJwtClaimsExtractorTest {

    private static final UUID USER_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String API_CLIENT_ID = "security-baseline-pdp";
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final BaselineJwtClaimsExtractor extractor = new BaselineJwtClaimsExtractor(
            API_CLIENT_ID, "email", "email_verified", scope);

    @Test
    void correo_verificado_se_normaliza_y_se_vincula_a_usuario_activo() {
        when(scope.findUsuarioIdByEmail("persona@uco.edu.co")).thenReturn(Optional.of(USER_ID));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(true);

        assertEquals(USER_ID, extractor.requireUserId(jwt(Map.of(
                "email", "  Persona@UCO.EDU.CO ", "email_verified", true))));
    }

    @Test
    void subject_ausente_correo_no_verificado_o_correo_ausente_rechazan() {
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "persona@uco.edu.co", "email_verified", true), null)));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "persona@uco.edu.co", "email_verified", false))));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email_verified", true))));
    }

    @Test
    void usuario_inexistente_inactivo_o_ambiguo_rechaza_sin_elegir_fila() {
        when(scope.findUsuarioIdByEmail("missing@uco.edu.co")).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "missing@uco.edu.co", "email_verified", true))));

        when(scope.findUsuarioIdByEmail("inactive@uco.edu.co")).thenReturn(Optional.of(USER_ID));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(false);
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "inactive@uco.edu.co", "email_verified", true))));

        when(scope.findUsuarioIdByEmail("duplicate@uco.edu.co"))
                .thenThrow(new InstitutionalIdentityAmbiguityException());
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "duplicate@uco.edu.co", "email_verified", true))));
    }

    @Test
    void solo_extrae_roles_del_cliente_institucional() {
        assertEquals(Set.of(InstitutionalRole.DOCENTE), extractor.extractRoles(jwt(Map.of(
                "email_verified", true,
                "resource_access", Map.of(
                        API_CLIENT_ID, Map.of("roles", List.of("DOCENTE")),
                        "account", Map.of("roles", List.of("ADMINISTRADOR")))))));
    }

    private static Jwt jwt(final Map<String, Object> claims) {
        return jwt(claims, "baseline-subject");
    }

    private static Jwt jwt(final Map<String, Object> claims, final String subject) {
        final Instant now = Instant.now();
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak.localhost:19090/realms/security-baseline")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300));
        if (subject != null) {
            builder.subject(subject);
        }
        claims.forEach(builder::claim);
        return builder.build();
    }
}
