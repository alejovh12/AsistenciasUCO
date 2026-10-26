package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver.AuthenticatedPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
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

    @Test
    void principal_incluye_uuid_y_solo_los_roles_institucionales_con_prefijo_role() {
        final TestingAuthenticationToken token = new TestingAuthenticationToken(
                USER_ID.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"), new SimpleGrantedAuthority("ROLE_COORDINADOR"),
                        new SimpleGrantedAuthority("ROLE_SUPERUSUARIO"), new SimpleGrantedAuthority("DOCENTE"),
                        new SimpleGrantedAuthority("SCOPE_email")));
        token.setAuthenticated(true);
        setAuthentication(token);

        final AuthenticatedPrincipal principal = provider.requireAuthenticatedPrincipal();

        assertEquals(USER_ID, principal.idUsuario());
        assertEquals(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR), principal.roles());
    }

    @Test
    void principal_sin_roles_institucionales_devuelve_conjunto_vacio() {
        setAuthentication(authenticated(USER_ID.toString()));

        assertEquals(Set.of(), provider.requireAuthenticatedPrincipal().roles());
    }

    @Test
    void principal_sin_authentication_o_con_nombre_invalido_lanza_forbidden() {
        SecurityContextHolder.getContext().setAuthentication(null);
        assertThrows(ForbiddenException.class, provider::requireAuthenticatedPrincipal);

        setAuthentication(authenticated("no-es-un-uuid"));
        assertThrows(ForbiddenException.class, provider::requireAuthenticatedPrincipal);
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
