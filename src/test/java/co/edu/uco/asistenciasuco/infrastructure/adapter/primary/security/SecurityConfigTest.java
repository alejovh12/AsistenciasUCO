package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;


import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.RequestActor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.ProtectedController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        SecurityConfigTest.ProtectedController.class,
        SecurityConfigTest.JwtDecoderTestConfig.class
})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:8081/realms/asistencias-uco",
        "spring.security.oauth2.resourceserver.jwt.audiences=asistencias-api",
        "spring.main.allow-bean-definition-overriding=true"
})
class SecurityConfigTest {

    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private ApiAccessDeniedHandler accessDeniedHandler;

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void token_con_issuer_correcto_y_audience_asistencias_api_es_aceptado() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-do")
                        .header(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value("usuario-123"))
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID));
    }

    @Test
    void token_con_audience_incorrecta_retorna_401_con_apiErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-audience")
                        .header(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.message").value(SecurityErrorCode.UNAUTHORIZED.defaultMessage()))
                .andExpect(jsonPath("$.path").value("/api/v1/protegido"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void request_sin_token_retorna_401_con_apiErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.message").value(SecurityErrorCode.UNAUTHORIZED.defaultMessage()))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void usuario_autenticado_expone_authority_role_do() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRoleDo").value(true));
    }

    @Test
    void realm_access_y_account_son_ignorados_como_roles() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ignored-roles-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleAuthorities.length()").value(0));
    }

    @Test
    void accessDeniedHandler_retorna_403_con_apiErrorResponse() throws Exception {
        CorrelationIdContext.set(UUID.fromString(CORRELATION_ID));
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertTrue(response.getContentAsString().contains("\"code\":\"FORBIDDEN\""));
        assertTrue(response.getContentAsString().contains(SecurityErrorCode.FORBIDDEN.defaultMessage()));
        assertFalse(response.getContentAsString().contains("denied"));
    }

    @Test
    void requestActorResolver_usa_idUsuario_como_actorId() {
        final Jwt jwt = jwt("valid-do", List.of(AUDIENCE), List.of("DO"));
        final AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protegido");
        request.setUserPrincipal(authentication);

        final RequestActor actor = new RequestActorResolver().resolve(request);

        assertEquals(AuditActorType.USER, actor.actorType());
        assertEquals("usuario-123", actor.actorId());
    }

    @Test
    void cors_acepta_localhost_4200() {
        final CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(corsRequest("http://localhost:4200"));

        assertEquals("http://localhost:4200", configuration.checkOrigin("http://localhost:4200"));
        assertTrue(configuration.getAllowedMethods().contains(HttpMethod.OPTIONS.name()));
        assertTrue(configuration.getAllowedHeaders().contains(HttpHeaders.AUTHORIZATION));
        assertTrue(configuration.getExposedHeaders().contains(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void cors_no_acepta_origen_arbitrario() throws Exception {
        mockMvc.perform(options("/api/v1/protegido")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequest corsRequest(final String origin) {
        final MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/api/v1/protegido");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
        return request;
    }

    private static Jwt jwt(final String tokenValue, final List<String> audiences, final List<String> clientRoles) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("keycloak-sub-123")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(audiences)
                .claim("idUsuario", "usuario-123")
                .claim("resource_access", Map.of(
                        "asistencias-api", Map.of("roles", clientRoles),
                        "account", Map.of("roles", List.of("manage-account"))
                ))
                .claim("realm_access", Map.of("roles", List.of("AD")))
                .build();
    }

    @RestController
    static final class ProtectedController {

        @GetMapping("/api/v1/protegido")
        Map<String, Object> protegido(final Authentication authentication) {
            final List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            final List<String> roleAuthorities = authorities.stream()
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .toList();
            return Map.of(
                    "principal", authentication.getName(),
                    "authorities", authorities,
                    "roleAuthorities", roleAuthorities,
                    "hasRoleDo", authorities.contains("ROLE_DO")
            );
        }
    }

    @TestConfiguration
    static class JwtDecoderTestConfig {

        @Bean
        JwtDecoder jwtDecoder(final OAuth2TokenValidator<Jwt> jwtValidator) {
            return token -> {
                final Jwt jwt = switch (token) {
                    case "valid-do" -> SecurityConfigTest.jwt(token, List.of(AUDIENCE), List.of("DO"));
                    case "ignored-roles-only" -> SecurityConfigTest.jwt(token, List.of(AUDIENCE), List.of());
                    case "wrong-audience" -> SecurityConfigTest.jwt(token, List.of("otra-api"), List.of("DO"));
                    default -> throw new BadJwtException("Unknown test token.");
                };
                final OAuth2TokenValidatorResult result = jwtValidator.validate(jwt);
                if (result.hasErrors()) {
                    throw new BadJwtException("Invalid test token.");
                }
                return jwt;
            };
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
