package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstitutionalJwtAuthenticationConverterTest {

    private static final UUID USER_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void principal_es_el_idUsuario_uuid() {
        final InstitutionalJwtAuthenticationConverter converter = converterReturning(Set.of());

        final JwtAuthenticationToken token = converter.convert(anyJwt());

        assertEquals(USER_ID.toString(), token.getName());
    }

    @Test
    void docente_se_mapea_a_role_docente() {
        final InstitutionalJwtAuthenticationConverter converter = converterReturning(Set.of(InstitutionalRole.DOCENTE));

        final JwtAuthenticationToken token = converter.convert(anyJwt());

        assertAuthorities(token, "ROLE_DOCENTE");
    }

    @Test
    void estudiante_se_mapea_a_role_estudiante() {
        final InstitutionalJwtAuthenticationConverter converter = converterReturning(Set.of(InstitutionalRole.ESTUDIANTE));

        final JwtAuthenticationToken token = converter.convert(anyJwt());

        assertAuthorities(token, "ROLE_ESTUDIANTE");
    }

    @Test
    void varios_roles_institucionales_se_mapean_todos() {
        final InstitutionalJwtAuthenticationConverter converter =
                converterReturning(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));

        final JwtAuthenticationToken token = converter.convert(anyJwt());

        assertAuthorities(token, "ROLE_DOCENTE", "ROLE_COORDINADOR");
    }

    @Test
    void ningun_rol_produce_cero_authorities() {
        final InstitutionalJwtAuthenticationConverter converter = converterReturning(Set.of());

        final JwtAuthenticationToken token = converter.convert(anyJwt());

        assertTrue(token.getAuthorities().isEmpty());
    }

    private static void assertAuthorities(final JwtAuthenticationToken token, final String... expected) {
        final Set<String> actual = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(expected), actual);
    }

    private InstitutionalJwtAuthenticationConverter converterReturning(final Set<InstitutionalRole> roles) {
        final JwtClaimsExtractor jwtClaimsExtractor = mock(JwtClaimsExtractor.class);
        when(jwtClaimsExtractor.requireUserId(org.mockito.ArgumentMatchers.any())).thenReturn(USER_ID);
        when(jwtClaimsExtractor.extractRoles(org.mockito.ArgumentMatchers.any())).thenReturn(roles);
        return new InstitutionalJwtAuthenticationConverter(jwtClaimsExtractor);
    }

    private static Jwt anyJwt() {
        final Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                .subject("keycloak-sub-123")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .claim("idUsuario", USER_ID.toString())
                .build();
    }
}
