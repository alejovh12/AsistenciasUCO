package co.edu.uco.asistenciasuco.openapi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwaggerUiDisabledRuntimeTest {

    private static final String SWAGGER_CONFIGURATION =
            "co.edu.uco.asistenciasuco.infrastructure.config.docs.SwaggerUiConfiguration";

    private MockMvc mockMvc;
    private AnnotationConfigWebApplicationContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "swagger-test", Map.of("asistencias.swagger-ui.enabled", "false")));
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
    void disabledPropertyDoesNotExposeUiAssetsOrCanonicalYaml() throws Exception {
        mockMvc.perform(get("/swagger-ui/"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/openapi/openapi-golden-path.yaml"))
                .andExpect(status().isNotFound());
    }

    @RestController
    static final class NoopController {

        @GetMapping("/__swagger-disabled-test-probe")
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
            // Baseline/disabled: ningún handler Swagger está registrado.
        }
    }
}
