package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityLink;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaselineJwtClaimsExtractorTest {

    private static final UUID USER_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String ISSUER = "http://keycloak.localhost:19090/realms/security-baseline";
    private static final String SUBJECT = "baseline-subject";
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ExternalIdentityLinkPort links = mock(ExternalIdentityLinkPort.class);
    private final BaselineJwtClaimsExtractor extractor = new BaselineJwtClaimsExtractor(
            API_CLIENT_ID, "email", "email_verified", "idUsuario", new ExternalIdentityResolver(scope, links));

    @Test
    void primera_vez_vincula_por_correo_verificado_normalizado_con_emisor_y_subject() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("persona@uco.edu.co")).thenReturn(Optional.of(USER_ID));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(true);
        when(links.link(ISSUER, SUBJECT, USER_ID, "persona@uco.edu.co")).thenReturn(true);

        assertEquals(USER_ID, extractor.requireUserId(jwt(Map.of(
                "email", "  Persona@UCO.EDU.CO ", "email_verified", true))));
        verify(links).link(ISSUER, SUBJECT, USER_ID, "persona@uco.edu.co");
    }

    @Test
    void con_vinculo_vigente_resuelve_por_issuer_y_subject_sin_leer_el_correo() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_ID, true)));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(true);

        assertEquals(USER_ID, extractor.requireUserId(jwt(Map.of("email", "cambiado@uco.edu.co"))));
        verify(scope, never()).findUsuarioIdByEmail(anyString());
    }

    @Test
    void subject_ausente_correo_no_verificado_o_correo_ausente_rechazan() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "persona@uco.edu.co", "email_verified", true), null)));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email", "persona@uco.edu.co", "email_verified", false))));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt(Map.of(
                "email_verified", true))));
    }

    @Test
    void usuario_inexistente_inactivo_ambiguo_revocado_o_en_conflicto_rechaza_como_token_invalido() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("missing@uco.edu.co")).thenReturn(Optional.empty());
        assertInvalidToken("missing@uco.edu.co");

        when(scope.findUsuarioIdByEmail("inactive@uco.edu.co")).thenReturn(Optional.of(USER_ID));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(false);
        assertInvalidToken("inactive@uco.edu.co");

        when(scope.findUsuarioIdByEmail("duplicate@uco.edu.co"))
                .thenThrow(new InstitutionalIdentityAmbiguityException());
        assertInvalidToken("duplicate@uco.edu.co");

        when(scope.findUsuarioIdByEmail("conflict@uco.edu.co")).thenReturn(Optional.of(USER_ID));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(true);
        when(links.link(ISSUER, SUBJECT, USER_ID, "conflict@uco.edu.co"))
                .thenThrow(new ExternalIdentityConflictException("ocupada"));
        assertInvalidToken("conflict@uco.edu.co");

        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_ID, false)));
        assertInvalidToken("revoked@uco.edu.co");
    }

    @Test
    void claim_idUsuario_solo_se_acepta_si_coincide_con_el_usuario_vinculado() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_ID, true)));
        when(scope.isUsuarioActivo(USER_ID)).thenReturn(true);

        assertEquals(USER_ID, extractor.requireUserId(jwt(Map.of("idUsuario", USER_ID.toString()))));
        assertEquals(USER_ID, extractor.requireUserId(jwt(Map.of("idUsuario", " " + USER_ID.toString().toUpperCase() + " "))));

        assertInvalidClaims(Map.of("idUsuario", UUID.randomUUID().toString()));
        assertInvalidClaims(Map.of("idUsuario", "no-es-uuid"));
        assertInvalidClaims(Map.of("idUsuario", 42));
    }

    @Test
    void solo_extrae_roles_del_cliente_institucional() {
        assertEquals(Set.of(InstitutionalRole.DOCENTE), extractor.extractRoles(jwt(Map.of(
                "email_verified", true,
                "resource_access", Map.of(
                        API_CLIENT_ID, Map.of("roles", List.of("DOCENTE")),
                        "security-baseline-pdp", Map.of("roles", List.of("ADMINISTRADOR")),
                        "account", Map.of("roles", List.of("ADMINISTRADOR")))))));
    }

    private void assertInvalidClaims(final Map<String, Object> claims) {
        final OAuth2AuthenticationException error = assertThrows(OAuth2AuthenticationException.class,
                () -> extractor.requireUserId(jwt(claims)));
        assertEquals("invalid_token", error.getError().getErrorCode());
    }

    private void assertInvalidToken(final String email) {
        final OAuth2AuthenticationException error = assertThrows(OAuth2AuthenticationException.class,
                () -> extractor.requireUserId(jwt(Map.of("email", email, "email_verified", true))));
        assertEquals("invalid_token", error.getError().getErrorCode());
    }

    private static Jwt jwt(final Map<String, Object> claims) {
        return jwt(claims, SUBJECT);
    }

    private static Jwt jwt(final Map<String, Object> claims, final String subject) {
        final Instant now = Instant.now();
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(ISSUER)
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
