package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 D.9-a (CONTRACT D §11.1, F-EST-024, OBS-17): {@code UnmappedRouteFilter} va despues de autenticar y antes del PEP.
 * Una ruta sin handler con token es 404 {@code RESOURCE_NOT_FOUND} sin consultar al PDP; sin token es 401; una ruta existente
 * sin permiso sigue siendo 403 (la decide el PEP).
 */
@WebMvcTest(controllers = UnmappedRouteFilterWebMvcTest.SondaController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        UnmappedRouteFilterWebMvcTest.SondaController.class,
        UnmappedRouteFilterWebMvcTest.Soporte.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class UnmappedRouteFilterWebMvcTest {

    private static final String AUDIENCE = "asistencias-api";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SecurityBaselinePepClient pepClient;

    @BeforeEach
    void reiniciar() {
        Mockito.reset(pepClient);
        when(pepClient.evaluate(any(), anyString())).thenReturn(new PepDecision(PepDecision.Outcome.FORBIDDEN, null));
    }

    @Test
    void ruta_sin_handler_con_token_es_404_sin_consultar_al_pdp() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias/11111111-1111-1111-1111-111111111111/prerrequisitos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ESTUDIANTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/estudiante/materias/11111111-1111-1111-1111-111111111111/prerrequisitos"));

        verify(pepClient, never()).evaluate(any(), anyString());
    }

    @Test
    void ruta_sin_handler_sin_token_es_401() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias/11111111-1111-1111-1111-111111111111/prerrequisitos"))
                .andExpect(status().isUnauthorized());

        verify(pepClient, never()).evaluate(any(), anyString());
    }

    @Test
    void ruta_existente_sin_permiso_sigue_siendo_403_por_el_pep() throws Exception {
        mockMvc.perform(get("/api/v1/sonda/existe").header(HttpHeaders.AUTHORIZATION, "Bearer ESTUDIANTE"))
                .andExpect(status().isForbidden());

        verify(pepClient).evaluate(any(), anyString());
    }

    @Test
    void ruta_existente_con_permiso_llega_al_controlador() throws Exception {
        when(pepClient.evaluate(any(), anyString())).thenReturn(new PepDecision(PepDecision.Outcome.ALLOW, "d-1"));

        mockMvc.perform(get("/api/v1/sonda/existe").header(HttpHeaders.AUTHORIZATION, "Bearer ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void ruta_existente_con_otro_metodo_no_es_404() throws Exception {
        when(pepClient.evaluate(any(), anyString())).thenReturn(new PepDecision(PepDecision.Outcome.ALLOW, "d-1"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/sonda/existe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ESTUDIANTE"))
                .andExpect(status().isMethodNotAllowed());
    }

    @RestController
    static class SondaController {
        @GetMapping("/api/v1/sonda/existe")
        String existe() {
            return "ok";
        }
    }

    @TestConfiguration
    static class Soporte {

        @Bean
        SecurityBaselinePepClient securityBaselinePepClient() {
            return Mockito.mock(SecurityBaselinePepClient.class);
        }

        @Bean
        SecurityPepEnforcementFilter securityPepEnforcementFilter(final SecurityBaselinePepClient client, final SecurityErrorResponseWriter writer) {
            return new SecurityPepEnforcementFilter(client, writer);
        }

        @Bean
        FilterRegistrationBean<SecurityPepEnforcementFilter> pepServletRegistration(final SecurityPepEnforcementFilter filter) {
            final FilterRegistrationBean<SecurityPepEnforcementFilter> registro = new FilterRegistrationBean<>(filter);
            registro.setEnabled(false);
            return registro;
        }

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(AUDIENCE, "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"ESTUDIANTE".equals(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", "22222222-2222-2222-2222-222222222222")
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", List.of(token))))
                        .build();
            };
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
