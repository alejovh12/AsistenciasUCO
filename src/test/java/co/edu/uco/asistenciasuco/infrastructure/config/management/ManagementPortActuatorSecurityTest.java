package co.edu.uco.asistenciasuco.infrastructure.config.management;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * VAL H2 (OBS-08): con servidores reales (API y gestion en puertos aleatorios distintos) y la cadena de seguridad real de la API
 * ({@link SecurityConfig}), Prometheus raspa {@code /actuator/prometheus} del puerto de gestion sin credenciales (antes 401),
 * {@code health} sigue abierto en gestion, cualquier otra ruta de gestion queda negada y el puerto de la API no expone Prometheus.
 */
@SpringBootTest(
        classes = ManagementPortActuatorSecurityTest.App.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.server.address=127.0.0.1",
                "management.endpoints.web.exposure.include=health,prometheus",
                "management.prometheus.metrics.export.enabled=true"
        })
class ManagementPortActuatorSecurityTest {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int puertoApi;

    @LocalManagementPort
    private int puertoGestion;

    @Autowired
    private MeterRegistry meterRegistry;

    private static HttpResponse<String> get(final int puerto, final String ruta) throws IOException, InterruptedException {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void los_puertos_de_api_y_gestion_son_distintos() {
        assertNotEquals(puertoApi, puertoGestion);
    }

    @Test
    void prometheus_responde_200_sin_credenciales_en_el_puerto_de_gestion() throws Exception {
        meterRegistry.counter("val_h2_scrape_prueba").increment();

        final HttpResponse<String> respuesta = get(puertoGestion, "/actuator/prometheus");

        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.body().contains("val_h2_scrape_prueba"), "El scrape debe traer las metricas del registro");
    }

    @Test
    void health_y_sus_sondas_siguen_abiertos_en_el_puerto_de_gestion() throws Exception {
        assertEquals(200, get(puertoGestion, "/actuator/health").statusCode());
    }

    @Test
    void el_puerto_de_la_api_no_expone_prometheus() throws Exception {
        final HttpResponse<String> respuesta = get(puertoApi, "/actuator/prometheus");

        assertEquals(401, respuesta.statusCode(), "Sin Bearer la API no deja ver la ruta (y Actuator no existe en ese puerto)");
        assertTrue(!respuesta.body().contains("val_h2") && !respuesta.body().contains("# HELP"));
    }

    @Test
    void otras_rutas_del_puerto_de_gestion_quedan_negadas() throws Exception {
        assertEquals(403, get(puertoGestion, "/actuator/env").statusCode());
        assertEquals(403, get(puertoGestion, "/actuator").statusCode());
        final HttpResponse<String> post = HTTP.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + puertoGestion + "/actuator/prometheus"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, post.statusCode(), "Solo GET esta permitido en gestion");
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            PropertyPlaceholderAutoConfiguration.class,
            TomcatServletWebServerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ServletWebSecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            EndpointAutoConfiguration.class,
            WebEndpointAutoConfiguration.class,
            ManagementContextAutoConfiguration.class,
            ServletManagementContextAutoConfiguration.class,
            TomcatServletManagementContextAutoConfiguration.class,
            HealthContributorAutoConfiguration.class,
            HealthContributorRegistryAutoConfiguration.class,
            HealthEndpointAutoConfiguration.class,
            MetricsAutoConfiguration.class,
            PrometheusMetricsExportAutoConfiguration.class
    })
    @Import({SecurityConfig.class, ManagementEndpointsSecurityConfiguration.class})
    static class App {

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return mock(JwtClaimsExtractor.class);
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint() {
            return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler() {
            return (request, response, denied) -> response.sendError(HttpStatus.FORBIDDEN.value());
        }
    }
}
