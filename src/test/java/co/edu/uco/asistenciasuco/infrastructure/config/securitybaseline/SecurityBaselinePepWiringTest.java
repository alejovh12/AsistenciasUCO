package co.edu.uco.asistenciasuco.infrastructure.config.securitybaseline;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline.SecurityBaselinePepConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPepEnforcementFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wiring real del enforcement baseline: {@link SecurityConfig} + {@link SecurityBaselinePepConfiguration}
 * con el provider activo, frente a un PEP simulado a nivel HTTP (servidor JDK en loopback). No sustituye la
 * prueba contra el proveedor real; certifica el registro del filtro, su orden y el mapeo HTTP observable.
 */
@WebMvcTest(controllers = SecurityBaselinePepWiringTest.ProbeController.class)
@Import({
        SecurityConfig.class,
        SecurityBaselinePepConfiguration.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        SecurityBaselinePepWiringTest.ProbeController.class,
        SecurityBaselinePepWiringTest.SupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "app.adapters.security.provider=baseline",
        "app.providers.security-baseline-pep.application-name=asistencias-uco-lab",
        "app.providers.security-baseline-pep.environment=dev",
        "app.providers.security-baseline-pep.application-credential=wiring-test-credential",
        "app.providers.security-baseline-pep.connect-timeout=1s",
        "app.providers.security-baseline-pep.request-timeout=700ms"
})
class SecurityBaselinePepWiringTest {

    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DECISION_ID = "8f6d7c1e-0a4b-4c2d-9e3f-112233445566";

    private static final HttpServer PEP = startPep();
    private static final AtomicReference<PepBehavior> BEHAVIOR = new AtomicReference<>();
    private static final ConcurrentLinkedQueue<CapturedCall> CALLS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger REDIRECT_TARGET_HITS = new AtomicInteger();
    private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Autowired
    private ListableBeanFactory beanFactory;

    @DynamicPropertySource
    static void pepUrl(final DynamicPropertyRegistry registry) {
        registry.add("app.providers.security-baseline-pep.pep-url",
                () -> "http://127.0.0.1:" + PEP.getAddress().getPort());
    }

    @AfterAll
    static void stopPep() {
        PEP.stop(0);
    }

    @BeforeEach
    void reset() {
        BEHAVIOR.set(PepBehavior.allow());
        CALLS.clear();
        REDIRECT_TARGET_HITS.set(0);
        HANDLER_CALLS.set(0);
    }

    // ── Registro y orden ────────────────────────────────────────────────────────────

    @Test
    void el_filtro_vive_una_sola_vez_en_la_cadena_despues_del_bearer_y_antes_de_authorization() {
        final List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
        assertEquals(1, chains.size());
        final List<Filter> filters = chains.getFirst().getFilters();
        final List<Integer> pepIndexes = indexesOf(filters, SecurityPepEnforcementFilter.class);
        assertEquals(1, pepIndexes.size(), "el filtro PEP debe registrarse exactamente una vez");
        final int pep = pepIndexes.getFirst();
        assertEquals(indexesOf(filters, BearerTokenAuthenticationFilter.class).getFirst() + 1, pep);
        assertTrue(pep < indexesOf(filters, AuthorizationFilter.class).getFirst());
    }

    @Test
    void spring_boot_no_registra_el_filtro_pep_como_filtro_servlet_global() {
        final Filter pepFilter = beanFactory.getBean(SecurityPepEnforcementFilter.class);
        int registrations = 0;
        for (final ServletContextInitializer initializer : new ServletContextInitializerBeans(beanFactory)) {
            if (initializer instanceof FilterRegistrationBean<?> registration && registration.getFilter() == pepFilter) {
                registrations++;
                assertFalse(registration.isEnabled(), "el registro servlet del filtro PEP debe estar deshabilitado");
            }
        }
        assertEquals(1, registrations);
    }

    // ── Decisiones observables ─────────────────────────────────────────────────────

    @Test
    void decision_permitida_llega_al_handler_una_sola_vez_y_expone_decision_id() throws Exception {
        final String correlationId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-do")
                        .header(HttpHeaders.COOKIE, "SESSION=must-not-forward")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Decision-Id", DECISION_ID));

        assertEquals(1, HANDLER_CALLS.get());
        assertEquals(1, CALLS.size(), "una sola evaluación PEP por petición");
        final CapturedCall call = CALLS.peek();
        assertEquals("POST", call.method());
        assertEquals("/internal/v1/embedded-access-decisions", call.path());
        assertEquals("Bearer valid-do", call.authorization());
        assertEquals("wiring-test-credential", call.credential());
        assertNull(call.cookie(), "nunca se reenvían cookies al PEP");
        assertNull(call.upgrade(), "HTTP/1.1 sin Upgrade h2c en el PEP baseline");
        assertEquals(correlationId, call.correlationId());
        assertTrue(call.body().contains("\"path\":\"/api/v1/protegido\""));
        assertTrue(call.body().contains("\"method\":\"GET\""));
        assertTrue(call.body().contains("\"applicationName\":\"asistencias-uco-lab\""));
        assertFalse(call.body().contains("valid-do"));
    }

    @Test
    void correlation_id_no_canonico_no_se_reenvia_crudo() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-do")
                        .header(CorrelationIdFilter.HEADER_NAME, "evil;value"))
                .andExpect(status().isOk());

        final String forwarded = CALLS.peek().correlationId();
        assertNotEquals("evil;value", forwarded);
        assertTrue(forwarded.matches("[0-9a-f-]{36}"));
    }

    @Test
    void denegacion_pep_responde_403_sin_ejecutar_el_handler() throws Exception {
        BEHAVIOR.set(PepBehavior.status(403));
        mockMvc.perform(get("/api/v1/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.FORBIDDEN.code()));
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void token_rechazado_por_pep_responde_401_sin_ejecutar_el_handler() throws Exception {
        BEHAVIOR.set(PepBehavior.status(401));
        mockMvc.perform(get("/api/v1/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.UNAUTHORIZED.code()));
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void fallo_y_respuestas_anomalas_del_pep_responden_503_fail_closed() throws Exception {
        final List<PepBehavior> anomalies = List.of(
                PepBehavior.status(500),
                PepBehavior.status(503),
                PepBehavior.status(200),               // 2xx sin decisión: fail-closed
                PepBehavior.noContentWithoutDecision(),
                PepBehavior.noContentWithDecision("bad id with spaces"),
                PepBehavior.redirect(),
                PepBehavior.slow());
        for (final PepBehavior anomaly : anomalies) {
            BEHAVIOR.set(anomaly);
            mockMvc.perform(get("/api/v1/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().doesNotExist("X-Decision-Id"))
                    .andExpect(jsonPath("$.code").value(SecurityErrorCode.ACCESS_DECISION_UNAVAILABLE.code()));
        }
        assertEquals(0, HANDLER_CALLS.get());
        assertEquals(0, REDIRECT_TARGET_HITS.get(), "nunca se sigue un redirect del PEP");
    }

    @Test
    void pep_inalcanzable_responde_503() throws Exception {
        BEHAVIOR.set(PepBehavior.closeConnection());
        mockMvc.perform(get("/api/v1/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void pep_permite_pero_rol_local_insuficiente_responde_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/solo-admin").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isForbidden());
        assertEquals(1, CALLS.size());
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void sin_token_o_token_invalido_responde_401_sin_consultar_al_pep() throws Exception {
        mockMvc.perform(get("/api/v1/protegido")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                .andExpect(status().isUnauthorized());
        assertEquals(0, CALLS.size());
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void rutas_publicas_y_preflight_no_consultan_al_pep() throws Exception {
        BEHAVIOR.set(PepBehavior.status(403));
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isOk());
        mockMvc.perform(options("/api/v1/protegido")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        assertEquals(0, CALLS.size());
    }

    @Test
    void ruta_fuera_de_prefijos_de_negocio_tambien_exige_decision() throws Exception {
        BEHAVIOR.set(PepBehavior.status(403));
        mockMvc.perform(get("/otra/ruta").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isForbidden());
        assertEquals(1, CALLS.size());
        assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void dispatch_async_no_reevalua_ni_salta_la_decision() throws Exception {
        final MvcResult started = mockMvc.perform(get("/api/v1/async")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(started)).andExpect(status().isOk());
        assertEquals(1, CALLS.size(), "el dispatch ASYNC hereda la decisión del REQUEST original");

        CALLS.clear();
        HANDLER_CALLS.set(0);
        BEHAVIOR.set(PepBehavior.status(403));
        mockMvc.perform(get("/api/v1/async").header(HttpHeaders.AUTHORIZATION, "Bearer valid-do"))
                .andExpect(status().isForbidden())
                .andExpect(request().asyncNotStarted());
        assertEquals(0, HANDLER_CALLS.get());
    }

    // ── Soporte ─────────────────────────────────────────────────────────────────────

    private static List<Integer> indexesOf(final List<Filter> filters, final Class<?> type) {
        final List<Integer> indexes = new java.util.ArrayList<>();
        for (int index = 0; index < filters.size(); index++) {
            if (type.isInstance(filters.get(index))) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private static HttpServer startPep() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/internal/v1/embedded-access-decisions", SecurityBaselinePepWiringTest::handle);
            server.createContext("/redirect-target", exchange -> {
                REDIRECT_TARGET_HITS.incrementAndGet();
                exchange.getResponseHeaders().add("X-Decision-Id", DECISION_ID);
                exchange.sendResponseHeaders(204, -1);
            });
            server.start();
            return server;
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void handle(final HttpExchange exchange) throws IOException {
        CALLS.add(new CapturedCall(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("X-Application-Credential"),
                exchange.getRequestHeaders().getFirst("Cookie"),
                exchange.getRequestHeaders().getFirst("X-Correlation-Id"),
                exchange.getRequestHeaders().getFirst("Upgrade"),
                new String(exchange.getRequestBody().readAllBytes())));
        BEHAVIOR.get().respond(exchange);
    }

    private record CapturedCall(String method, String path, String authorization, String credential,
                                String cookie, String correlationId, String upgrade, String body) {
    }

    @FunctionalInterface
    private interface PepBehavior {
        void respond(HttpExchange exchange) throws IOException;

        static PepBehavior allow() {
            return noContentWithDecision(DECISION_ID);
        }

        static PepBehavior noContentWithDecision(final String decisionId) {
            return exchange -> {
                exchange.getResponseHeaders().add("X-Decision-Id", decisionId);
                exchange.sendResponseHeaders(204, -1);
            };
        }

        static PepBehavior noContentWithoutDecision() {
            return exchange -> exchange.sendResponseHeaders(204, -1);
        }

        static PepBehavior status(final int status) {
            return exchange -> exchange.sendResponseHeaders(status, -1);
        }

        static PepBehavior redirect() {
            return exchange -> {
                exchange.getResponseHeaders().add("Location",
                        "http://127.0.0.1:" + PEP.getAddress().getPort() + "/redirect-target");
                exchange.sendResponseHeaders(307, -1);
            };
        }

        static PepBehavior slow() {
            return exchange -> {
                try {
                    Thread.sleep(2_000);
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                exchange.getResponseHeaders().add("X-Decision-Id", DECISION_ID);
                exchange.sendResponseHeaders(204, -1);
            };
        }

        static PepBehavior closeConnection() {
            return HttpExchange::close;
        }
    }

    @RestController
    static final class ProbeController {

        @GetMapping("/api/v1/protegido")
        Map<String, Object> protegido() {
            HANDLER_CALLS.incrementAndGet();
            return Map.of("ok", true);
        }

        @GetMapping("/api/v1/admin/solo-admin")
        Map<String, Object> soloAdmin() {
            HANDLER_CALLS.incrementAndGet();
            return Map.of("ok", true);
        }

        @GetMapping("/otra/ruta")
        Map<String, Object> otraRuta() {
            HANDLER_CALLS.incrementAndGet();
            return Map.of("ok", true);
        }

        @GetMapping("/api/v1/async")
        Callable<Map<String, Object>> async() {
            HANDLER_CALLS.incrementAndGet();
            return () -> Map.of("ok", true);
        }

        @GetMapping("/actuator/health")
        Map<String, Object> health() {
            return Map.of("status", "UP");
        }
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
                if (!"valid-do".equals(token)) {
                    throw new BadJwtException("Unknown test token.");
                }
                final Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer(ISSUER)
                        .subject("keycloak-sub-wiring")
                        .issuedAt(now.minusSeconds(30))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", USER_ID.toString())
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", List.of("DOCENTE"))))
                        .build();
            };
        }

        @Bean
        co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> java.util.Optional.empty();
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
