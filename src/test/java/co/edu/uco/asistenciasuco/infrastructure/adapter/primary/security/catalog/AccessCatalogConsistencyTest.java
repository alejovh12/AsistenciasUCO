package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog.SpringMvcEndpointInventory.Endpoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consistencia entre tres fuentes que deben decir lo mismo:
 * <ol>
 *   <li>los endpoints que Spring MVC registra de verdad ({@link SpringMvcEndpointInventory});</li>
 *   <li>la definición compartida {@code access-catalog.json}, que {@code provision_asistencias.py} replica en el PDP;</li>
 *   <li>el {@code SecurityFilterChain} real ({@link SecurityConfig}).</li>
 * </ol>
 * Un endpoint nuevo sin fila en el catálogo, una fila sin endpoint, o un rol que el catálogo y Spring entiendan
 * distinto, rompen esta prueba: un endpoint sin regla nunca recibe acceso por defecto.
 */
@WebMvcTest(controllers = AccessCatalogConsistencyTest.ProbeController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        AccessCatalogConsistencyTest.ProbeController.class,
        AccessCatalogConsistencyTest.SupportConfig.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class AccessCatalogConsistencyTest {

    private static final String NO_ROLE_TOKEN = "SIN_ROL";
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");
    private static final Set<String> VERBS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";

    private static final Set<String> ROLE_NAMES = new TreeSet<>(
            Arrays.stream(InstitutionalRole.values()).map(Enum::name).toList());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void el_catalogo_define_exactamente_los_endpoints_que_spring_registra() {
        final AccessCatalog catalog = AccessCatalog.load();
        final Set<Endpoint> registered = SpringMvcEndpointInventory.discover();
        final Set<Endpoint> exempt = new TreeSet<>();
        catalog.exempt().forEach(e -> exempt.add(new Endpoint(e.method(), e.path())));
        final Set<Endpoint> cataloged = new TreeSet<>();
        catalog.endpoints().forEach(e -> cataloged.add(e.endpoint()));

        final Set<Endpoint> withoutRule = new TreeSet<>(registered);
        withoutRule.removeAll(cataloged);
        withoutRule.removeAll(exempt);
        final Set<Endpoint> stale = new TreeSet<>(cataloged);
        stale.removeAll(registered);
        final Set<Endpoint> staleExempt = new TreeSet<>(exempt);
        staleExempt.removeAll(registered);

        assertTrue(withoutRule.isEmpty(), () -> "Endpoints SIN regla en infra/security-baseline/access-catalog.json "
                + "(definir roles y registrar en el PDP; nunca acceso por defecto): " + withoutRule);
        assertTrue(stale.isEmpty(), () -> "Reglas del catálogo sin endpoint en Spring MVC (obsoletas): " + stale);
        assertTrue(staleExempt.isEmpty(), () -> "Exenciones sin endpoint en Spring MVC: " + staleExempt);
        assertTrue(registered.stream().noneMatch(e -> "ANY".equals(e.method())),
                () -> "Mapeos sin método HTTP explícito (aceptarían cualquier verbo): "
                        + registered.stream().filter(e -> "ANY".equals(e.method())).toList());
    }

    @Test
    void el_catalogo_es_estricto_sin_duplicados_comodines_ni_roles_ajenos() {
        final AccessCatalog catalog = AccessCatalog.load();
        final List<String> problems = new ArrayList<>();
        final Set<Endpoint> seen = new HashSet<>();

        assertTrue(Set.copyOf(catalog.roles()).equals(ROLE_NAMES) && catalog.roles().size() == ROLE_NAMES.size(),
                "El catálogo debe declarar exactamente los cinco roles institucionales");
        for (final AccessCatalog.Entry entry : catalog.endpoints()) {
            final String id = entry.method() + " " + entry.path();
            if (!seen.add(entry.endpoint())) {
                problems.add("duplicado: " + id);
            }
            if (!VERBS.contains(entry.method())) {
                problems.add("método inválido: " + id);
            }
            if (!entry.path().startsWith("/api/") || entry.path().contains("*")) {
                problems.add("ruta comodín o fuera de /api: " + id);
            }
            if (entry.roles() == null || entry.roles().isEmpty() || !ROLE_NAMES.containsAll(entry.roles())
                    || entry.roles().size() != Set.copyOf(entry.roles()).size()) {
                problems.add("roles vacíos, repetidos o ajenos: " + id);
                continue;
            }
            final boolean allRoles = entry.roles().size() == ROLE_NAMES.size();
            if (allRoles != "AUTHENTICATED".equals(entry.access())) {
                problems.add("access debe ser AUTHENTICATED si y solo si concede los cinco roles (convenience-all): " + id);
            }
        }
        for (final AccessCatalog.Exempt exempt : catalog.exempt()) {
            if (seen.contains(new Endpoint(exempt.method(), exempt.path())) || exempt.reason() == null
                    || exempt.reason().isBlank()) {
                problems.add("exención inválida (en el catálogo o sin motivo): " + exempt.method() + " " + exempt.path());
            }
        }
        final Set<String> profileNames = new HashSet<>();
        final Set<String> coveredRoles = new HashSet<>();
        for (final AccessCatalog.Profile profile : catalog.profiles()) {
            if (!profileNames.add(profile.name()) || profile.name().length() < 3 || profile.name().length() > 60) {
                problems.add("perfil duplicado o con nombre fuera de 3-60 caracteres: " + profile.name());
            }
            if (profile.roles() == null || profile.roles().isEmpty() || !ROLE_NAMES.containsAll(profile.roles())) {
                problems.add("perfil vacío o con roles ajenos: " + profile.name());
            } else {
                coveredRoles.addAll(profile.roles());
            }
        }
        if (!coveredRoles.equals(ROLE_NAMES)) {
            problems.add("cada rol institucional debe pertenecer a algún perfil; faltan: "
                    + ROLE_NAMES.stream().filter(role -> !coveredRoles.contains(role)).toList());
        }
        assertTrue(problems.isEmpty(), () -> "Catálogo inválido: " + problems);
    }

    @Test
    void el_securityfilterchain_real_autoriza_cada_endpoint_como_dice_el_catalogo() throws Exception {
        final AccessCatalog catalog = AccessCatalog.load();
        final List<String> divergences = new ArrayList<>();

        for (final AccessCatalog.Entry entry : catalog.endpoints()) {
            final String concretePath = PATH_VARIABLE.matcher(entry.path()).replaceAll(UUID.randomUUID().toString());
            for (final String role : ROLE_NAMES) {
                final boolean allowedBySpring = !isDenied(call(entry.method(), concretePath, role));
                final boolean allowedByCatalog = entry.roles().contains(role);
                if (allowedBySpring != allowedByCatalog) {
                    divergences.add(entry.method() + " " + entry.path() + " rol " + role
                            + ": Spring " + (allowedBySpring ? "PERMITE" : "NIEGA")
                            + ", catálogo " + (allowedByCatalog ? "PERMITE" : "NIEGA"));
                }
            }
            if (call(entry.method(), concretePath, null) != 401) {
                divergences.add(entry.method() + " " + entry.path() + ": sin Bearer debe responder 401");
            }
            final boolean noRoleAllowed = !isDenied(call(entry.method(), concretePath, NO_ROLE_TOKEN));
            if (noRoleAllowed != "AUTHENTICATED".equals(entry.access())) {
                divergences.add(entry.method() + " " + entry.path() + ": usuario autenticado SIN rol institucional "
                        + (noRoleAllowed ? "pasa Spring" : "es negado") + " pero access=" + entry.access());
            }
        }
        assertTrue(divergences.isEmpty(), () -> "SecurityConfig y catálogo divergen:\n" + String.join("\n", divergences));
    }

    private int call(final String method, final String path, final String bearerRoleToken) throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (bearerRoleToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerRoleToken);
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    /** 401/403 son denegaciones de seguridad; cualquier otro código (404 sin handler) significa que la cadena dejó pasar. */
    private static boolean isDenied(final int status) {
        return status == 401 || status == 403;
    }

    @RestController
    static final class ProbeController {

        @GetMapping("/__access-catalog-probe")
        String probe() {
            return "ok";
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
            final OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(AUDIENCE);
            return token -> {
                if (!ROLE_NAMES.contains(token) && !NO_ROLE_TOKEN.equals(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                final List<String> roles = ROLE_NAMES.contains(token) ? List.of(token) : List.of();
                final Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer(ISSUER)
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .notBefore(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", USER_ID)
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", roles)))
                        .build();
                final OAuth2TokenValidatorResult result = audienceValidator.validate(jwt);
                if (result.hasErrors()) {
                    throw new BadJwtException("Invalid test token.");
                }
                return jwt;
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
