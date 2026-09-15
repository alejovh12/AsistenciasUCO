package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link SecurityContextAuthenticatedUserResolver} solo debe leer {@code Authentication.getName()}
 * (ya normalizado por {@link InstitutionalJwtAuthenticationConverter}) — nunca claims de Jwt,
 * nunca Keycloak, nunca la base de datos.
 */
class JwtAuthenticatedUserProviderTest {

    private static final UUID USER_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    private final SecurityContextAuthenticatedUserResolver provider = new SecurityContextAuthenticatedUserResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authentication_name_uuid_valido_se_retorna() {
        setAuthentication(authenticated(USER_ID.toString()));

        assertEquals(USER_ID, provider.requireAuthenticatedUserId());
    }

    @Test
    void sin_authentication_lanza_forbidden() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThrows(ForbiddenException.class, provider::requireAuthenticatedUserId);
    }

    @Test
    void authentication_no_autenticada_lanza_forbidden() {
        final TestingAuthenticationToken token = new TestingAuthenticationToken(USER_ID.toString(), null);
        token.setAuthenticated(false);
        setAuthentication(token);

        assertThrows(ForbiddenException.class, provider::requireAuthenticatedUserId);
    }

    @Test
    void principal_invalido_lanza_forbidden() {
        setAuthentication(authenticated("no-es-un-uuid"));

        assertThrows(ForbiddenException.class, provider::requireAuthenticatedUserId);
    }

    private static Authentication authenticated(final String principal) {
        final TestingAuthenticationToken token = new TestingAuthenticationToken(principal, null);
        token.setAuthenticated(true);
        return token;
    }

    private static void setAuthentication(final Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
