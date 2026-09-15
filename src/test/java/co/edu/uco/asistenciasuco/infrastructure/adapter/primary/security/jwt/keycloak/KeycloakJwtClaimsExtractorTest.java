package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakJwtClaimsAdapterTest {

    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String VALID_USER_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";

    private final KeycloakJwtClaimsExtractor adapter = new KeycloakJwtClaimsExtractor(API_CLIENT_ID, USER_ID_CLAIM);

    @Test
    void idUsuario_valido_se_retorna_como_uuid() {
        final Jwt jwt = jwtWithClaims(Map.of(USER_ID_CLAIM, VALID_USER_ID));

        assertEquals(VALID_USER_ID, adapter.requireUserId(jwt).toString());
    }

    @Test
    void idUsuario_ausente_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of());

        assertThrows(OAuth2AuthenticationException.class, () -> adapter.requireUserId(jwt));
    }

    @Test
    void idUsuario_blank_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of(USER_ID_CLAIM, "   "));

        assertThrows(OAuth2AuthenticationException.class, () -> adapter.requireUserId(jwt));
    }

    @Test
    void idUsuario_no_uuid_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of(USER_ID_CLAIM, "no-es-un-uuid"));

        assertThrows(OAuth2AuthenticationException.class, () -> adapter.requireUserId(jwt));
    }

    @Test
    void resource_access_correcto_con_api_client_presente_extrae_roles() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("DOCENTE")))
        ));

        assertEquals(Set.of(InstitutionalRole.DOCENTE), adapter.extractRoles(jwt));
    }

    @Test
    void api_client_ausente_en_resource_access_retorna_roles_vacios() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of("otro-client", Map.of("roles", List.of("DOCENTE")))
        ));

        assertTrue(adapter.extractRoles(jwt).isEmpty());
    }

    @Test
    void roles_validos_se_mapean_a_institutional_role() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("DOCENTE", "COORDINADOR")))
        ));

        assertEquals(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR), adapter.extractRoles(jwt));
    }

    @Test
    void roles_desconocidos_se_ignoran() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of(
                        "roles", List.of("DOCENTE", "offline_access", "uma_authorization", "default-roles-asistencias-uco")
                ))
        ));

        assertEquals(Set.of(InstitutionalRole.DOCENTE), adapter.extractRoles(jwt));
    }

    @Test
    void codigos_cortos_de_keycloak_ya_no_se_aceptan() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("AD", "DE", "CD", "DO", "ES")))
        ));

        assertTrue(adapter.extractRoles(jwt).isEmpty());
    }

    @Test
    void prefijo_role_no_se_acepta_como_rol_de_token() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("ROLE_DOCENTE", "ROLE_ESTUDIANTE")))
        ));

        assertTrue(adapter.extractRoles(jwt).isEmpty());
    }

    @Test
    void variantes_de_mayusculas_no_se_aceptan_mapping_es_exacto() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("docente", "Docente")))
        ));

        assertTrue(adapter.extractRoles(jwt).isEmpty());
    }

    @Test
    void roles_de_otro_client_se_ignoran() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(
                        API_CLIENT_ID, Map.of("roles", List.of("DOCENTE")),
                        "account", Map.of("roles", List.of("manage-account"))
                )
        ));

        assertEquals(Set.of(InstitutionalRole.DOCENTE), adapter.extractRoles(jwt));
    }

    @Test
    void realm_access_se_ignora_para_autorizacion() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "realm_access", Map.of("roles", List.of("ADMINISTRADOR")),
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of()))
        ));

        assertTrue(adapter.extractRoles(jwt).isEmpty());
    }

    @Test
    void resource_access_con_tipo_incorrecto_no_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of("resource_access", "esto-no-es-un-mapa"));

        assertEquals(Set.of(), adapter.extractRoles(jwt));
    }

    @Test
    void resource_access_client_entry_con_tipo_incorrecto_no_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of("resource_access", Map.of(API_CLIENT_ID, "esto-no-es-un-mapa")));

        assertEquals(Set.of(), adapter.extractRoles(jwt));
    }

    @Test
    void roles_con_tipo_incorrecto_no_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", "esto-no-es-una-lista"))
        ));

        assertEquals(Set.of(), adapter.extractRoles(jwt));
    }

    @Test
    void roles_con_elementos_de_tipo_incorrecto_se_ignoran_sin_lanzar_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of(
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("DOCENTE", 42, Map.of())))
        ));

        assertEquals(Set.of(InstitutionalRole.DOCENTE), adapter.extractRoles(jwt));
    }

    private static Jwt jwtWithClaims(final Map<String, Object> extraClaims) {
        final Instant now = Instant.now();
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                .subject("keycloak-sub-123")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300));
        extraClaims.forEach(builder::claim);
        return builder.build();
    }
}
