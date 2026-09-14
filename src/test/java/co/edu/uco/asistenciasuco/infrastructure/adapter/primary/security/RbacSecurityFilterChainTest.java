package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.keycloak.KeycloakJwtClaimsAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.spi.JwtClaimsAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditEventPublisher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el ORDEN y CONTENIDO de los matchers de {@link SecurityConfig} — no la lógica de
 * ningún UseCase real. Por eso usa un controller sonda ({@link RbacProbeController}) mapeado
 * a las rutas reales, en lugar de los controllers de producción (que requerirían mockear
 * muchos InputPorts sin aportar nada a esta verificación).
 *
 * <p>Política vigente verificada aquí (inspeccionada en {@code SecurityConfig} antes de
 * escribir las expectativas, no inventada):</p>
 * <ul>
 *   <li>{@code /api/v1/docentes/**} (directorio general, {@code DocenteController}): solo
 *       {@code COORDINADOR}/{@code ADMINISTRADOR} — ver duda documentada en
 *       docs/security/runtime-security-provider-architecture.md (no hay contrato funcional
 *       explícito; DOCENTE NO tiene acceso bajo la política actual).</li>
 *   <li>{@code POST /api/v1/grupos} (command de coordinación): {@code COORDINADOR}/{@code ADMINISTRADOR}.</li>
 *   <li>{@code POST /api/v1/asistencias/lote} (registro docente por lote): {@code DOCENTE}.</li>
 *   <li>{@code /api/v1/estudiante/**} (portal del propio estudiante): {@code ESTUDIANTE}.</li>
 * </ul>
 */
@WebMvcTest(controllers = RbacSecurityFilterChainTest.RbacProbeController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        RbacSecurityFilterChainTest.RbacProbeController.class,
        RbacSecurityFilterChainTest.RbacTestSupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class RbacSecurityFilterChainTest {

    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    // --- /api/v1/docentes/** : directorio general (COORDINADOR/ADMINISTRADOR) ---

    @Test
    void docentes_directorio_estudiante_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void docentes_directorio_docente_recibe_403_bajo_politica_actual() throws Exception {
        // Política vigente: DOCENTE no administra el directorio general de docentes.
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void docentes_directorio_coordinador_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- POST /api/v1/grupos : command de coordinación ---

    @Test
    void crear_grupo_estudiante_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_grupo_coordinador_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void peticion_insegura_solo_con_cookie_requiere_csrf() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").cookie(new Cookie("JSESSIONID", "session-de-prueba")))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticion_insegura_con_bearer_y_cookie_sigue_permitida() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")
                        .cookie(new Cookie("JSESSIONID", "session-de-prueba"))
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- POST /api/v1/asistencias/lote : registro docente por lote ---

    @Test
    void registrar_asistencias_lote_estudiante_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/lote").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_asistencias_lote_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/lote").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    // --- /api/v1/estudiantes/** : directorio institucional general (distinto del portal
    // propio en /estudiante/**), restringido en este mismo prompt correctivo ---

    @Test
    void estudiantes_directorio_docente_recibe_403() throws Exception {
        // El docente tiene su vertical especifica y acotada por grupo
        // (GET /api/v1/grupos/{grupoId}/estudiantes); el directorio general no se le abre.
        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiantes_directorio_coordinador_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- /api/v1/estudiante/** : portal del propio estudiante ---

    @Test
    void portal_estudiante_docente_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void portal_estudiante_estudiante_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isOk());
    }

    private static String bearer(final String institutionalRoleName) {
        return "Bearer " + institutionalRoleName;
    }

    private static Jwt jwtFor(final String institutionalRoleName) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue(institutionalRoleName)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("keycloak-sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(List.of(AUDIENCE))
                .claim(USER_ID_CLAIM, USER_ID)
                .claim("resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of(institutionalRoleName))))
                .build();
    }

    @RestController
    static final class RbacProbeController {

        @GetMapping("/api/v1/docentes")
        String docentes() {
            return "ok";
        }

        @GetMapping("/api/v1/estudiantes")
        String estudiantes() {
            return "ok";
        }

        @PostMapping("/api/v1/grupos")
        String crearGrupo() {
            return "ok";
        }

        @PostMapping("/api/v1/asistencias/lote")
        String registrarAsistenciasLote() {
            return "ok";
        }

        @GetMapping("/api/v1/estudiante/materias")
        String materiasEstudiante() {
            return "ok";
        }
    }

    @TestConfiguration
    static class RbacTestSupportConfig {

        @Bean
        JwtClaimsAdapter jwtClaimsAdapter() {
            return new KeycloakJwtClaimsAdapter(API_CLIENT_ID, USER_ID_CLAIM);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            final OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(AUDIENCE);
            return token -> {
                final Jwt jwt = jwtFor(token);
                final OAuth2TokenValidatorResult result = audienceValidator.validate(jwt);
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
