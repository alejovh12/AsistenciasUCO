package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.ConsultarPerfilInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.UsuarioPerfilController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog.SpringMvcEndpointInventory.Endpoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * VAL ROL-05 (decision de John, 2026-11-12): {@code POST /api/v1/usuarios} creaba un usuario y su cuenta en el proveedor de
 * identidad para cualquier usuario autenticado, sin ejecutor ni ambito. Se retiro. Esta prueba fija que la ruta ya no
 * existe para ningun rol (la cadena real de seguridad deja pasar al autenticado y Spring MVC responde 404 o 405, sin
 * invocar ningun caso de uso), que no queda en el inventario de endpoints ni en el catalogo de acceso, y que el caso de
 * uso, el controlador y el comando del repositorio que la servian no volvieron.
 */
@WebMvcTest(controllers = UsuarioPerfilController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        AltaUsuarioSinAmbitoRetiradaTest.SupportConfig.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class AltaUsuarioSinAmbitoRetiradaTest {

    private static final String RUTA = "/api/v1/usuarios";
    private static final String SIN_ROL = "SIN_ROL";
    private static final String AUDIENCE = "asistencias-api";
    private static final Set<String> ROLES = Set.copyOf(
            Arrays.stream(InstitutionalRole.values()).map(Enum::name).toList());
    private static final String CUERPO = """
            {"tipoIdIdentificacion":"11111111-1111-1111-1111-111111111111","numeroIdentificacion":123456789,
             "primerNombre":"Ana","primerApellido":"Perez","correo":"ana.rol05@uco.edu.co","password":"NoSeUsa-ROL05"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarPerfilInputPort consultarPerfilInputPort;

    @Test
    void ROL05_01_post_usuarios_no_existe_para_ningun_rol_y_no_invoca_casos_de_uso() throws Exception {
        final List<String> problemas = new ArrayList<>();
        final List<String> tokens = new ArrayList<>(ROLES);
        tokens.add(SIN_ROL);
        for (final String token : tokens) {
            final int status = mockMvc.perform(post(RUTA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO))
                    .andReturn().getResponse().getStatus();
            if (status != 404 && status != 405) {
                problemas.add(token + " -> HTTP " + status);
            }
        }
        assertTrue(problemas.isEmpty(), () -> "POST " + RUTA + " debe responder 404 o 405 a todo rol: " + problemas);
        verifyNoInteractions(consultarPerfilInputPort);
    }

    @Test
    void ROL05_02_sin_bearer_sigue_respondiendo_401() throws Exception {
        final int status = mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 401, () -> "Sin Bearer se esperaba 401 y fue " + status);
    }

    @Test
    void ROL05_03_la_ruta_no_esta_en_el_inventario_de_spring_ni_en_el_catalogo_de_acceso() {
        final Endpoint retirada = new Endpoint("POST", RUTA);
        assertFalse(SpringMvcEndpointInventory.discover().contains(retirada),
                "Ningun controlador debe volver a mapear POST " + RUTA);
        assertTrue(AccessCatalog.load().endpoints().stream().noneMatch(e -> retirada.equals(e.endpoint())),
                "access-catalog.json no debe declarar POST " + RUTA);
    }

    @Test
    void ROL05_04_el_caso_de_uso_el_controlador_y_el_comando_del_repositorio_no_volvieron() {
        for (final String clase : List.of(
                "co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.UsuarioController",
                "co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.impl.ProvisionarUsuarioUseCaseImpl",
                "co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl.CrearUsuarioUseCaseImpl")) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(clase), clase + " debia retirarse");
        }
        assertTrue(Arrays.stream(UsuarioRepositoryPort.class.getMethods()).noneMatch(m -> "crearUsuario".equals(m.getName())),
                "UsuarioRepositoryPort no debe exponer el alta generica crearUsuario (usp_sincronizar_usuario)");
    }

    @TestConfiguration
    static class SupportConfig {

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(AUDIENCE, "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!ROLES.contains(token) && !SIN_ROL.equals(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                final List<String> roles = ROLES.contains(token) ? List.of(token) : List.of();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .notBefore(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", "22222222-2222-2222-2222-222222222222")
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", roles)))
                        .build();
            };
        }

        @Bean
        AuthenticatedUserResolver authenticatedUserResolver() {
            return () -> UUID.fromString("22222222-2222-2222-2222-222222222222");
        }

        @Bean
        ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> Optional.empty();
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
