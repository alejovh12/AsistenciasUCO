package co.edu.uco.asistenciasuco.openapi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwaggerUiEnabledRuntimeTest {

    private static final String SWAGGER_CONFIGURATION =
            "co.edu.uco.asistenciasuco.infrastructure.config.docs.SwaggerUiConfiguration";
    private static final Path CANONICAL_OPENAPI = Path.of(
            "docs", "contracts", "openapi", "openapi-golden-path.yaml");

    private MockMvc mockMvc;
    private AnnotationConfigWebApplicationContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "swagger-test", java.util.Map.of("asistencias.swagger-ui.enabled", "true")));
        context.register(MvcTestConfiguration.class);
        registerSwaggerConfigurationWhenPresent(context);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void swaggerUiRouteRedirectsToStableIndexAndLoadsOfflineAssets() throws Exception {
        mockMvc.perform(get("/swagger-ui/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("AsistenciasUCO API")));

        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk());
    }

    @Test
    void runtimeOpenApiIsTheCanonicalYamlByteForByte() throws Exception {
        final byte[] canonical = Files.readAllBytes(CANONICAL_OPENAPI);
        final var response = mockMvc.perform(get("/openapi/openapi-golden-path.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertTrue(
                response.getContentType().contains("yaml")
                        || response.getContentType().equals(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                () -> "Content-Type YAML inesperado: " + response.getContentType());
        assertArrayEquals(canonical, response.getContentAsByteArray(),
                "El recurso runtime debe ser byte-for-byte igual al OpenAPI canónico");
    }

    @Test
    void swaggerInitializerPointsOnlyToCanonicalRuntimeOpenApiWithSafeOptions() throws Exception {
        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "url: '/openapi/openapi-golden-path.yaml'")))
                .andExpect(content().string(containsString("deepLinking: true")))
                .andExpect(content().string(containsString("displayOperationId: true")))
                .andExpect(content().string(containsString("tryItOutEnabled: true")))
                .andExpect(content().string(containsString("docExpansion: 'list'")))
                .andExpect(content().string(containsString("persistAuthorization: false")))
                .andExpect(content().string(not(containsString("/v3/api-docs"))))
                .andExpect(content().string(not(containsString("https://"))))
                .andExpect(content().string(not(containsString("http://"))));
    }

    @Test
    void generatedCodeFirstEndpointIsAbsent() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @RestController
    static final class NoopController {

        @GetMapping("/__swagger-test-probe")
        String probe() {
            return "ok";
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcTestConfiguration {

        @Bean
        NoopController noopController() {
            return new NoopController();
        }
    }

    private static void registerSwaggerConfigurationWhenPresent(
            final AnnotationConfigWebApplicationContext applicationContext
    ) throws Exception {
        try {
            applicationContext.register(Class.forName(SWAGGER_CONFIGURATION));
        } catch (ClassNotFoundException ignored) {
            // RED: la configuración productiva todavía no existe.
        }
    }
}
