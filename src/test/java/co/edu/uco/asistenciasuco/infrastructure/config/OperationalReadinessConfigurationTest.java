package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.infrastructure.config.resilience.ResilienceCircuitBreakerNames;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuracion operativa del backend, leida del {@code application.yml} real:
 * <ul>
 *   <li>F12-09: apagado elegante con tope por fase y {@code HEALTHCHECK} en el Dockerfile.</li>
 *   <li>OBS-10: sondas de liveness y readiness con BD y circuit breakers; histograma de latencia HTTP.</li>
 *   <li>F12-11: logs a stdout por defecto (JSON) y archivo solo si se define la variable.</li>
 *   <li>REA-02: los circuit breakers del PEP, MinIO y ClamAV se construyen desde la configuracion.</li>
 *   <li>F12-08: el pool de conexiones (Hikari) se dimensiona por variable y por replica.</li>
 * </ul>
 */
class OperationalReadinessConfigurationTest {

    private static PropertySource<?> yaml;

    @BeforeAll
    static void cargarYaml() throws IOException {
        final List<PropertySource<?>> fuentes = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource("src/main/resources/application.yml"));
        yaml = fuentes.getFirst();
    }

    @Test
    void f12_09_apagado_elegante_con_tope_por_fase() {
        assertEquals("graceful", valor("server.shutdown"));
        assertEquals("${SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE:20s}", valor("spring.lifecycle.timeout-per-shutdown-phase"));
    }

    @Test
    void obs_10_sondas_de_liveness_y_readiness_con_bd_y_breakers() {
        assertEquals("true", valor("management.endpoint.health.probes.enabled"));
        assertEquals("livenessState", valor("management.endpoint.health.group.liveness.include"));
        assertEquals("readinessState,db,circuitBreakers", valor("management.endpoint.health.group.readiness.include"));
        assertEquals("true", valor("management.health.circuitbreakers.enabled"));
        // Las sondas viven en el puerto de gestion (loopback por defecto), nunca en el de la API.
        assertEquals("${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}", valor("management.server.address"));
    }

    @Test
    void latencia_p95_tiene_histograma_de_http_server_requests() {
        assertEquals("true", valor("management.metrics.distribution.percentiles-histogram.http.server.requests"));
    }

    @Test
    void f12_11_logs_a_stdout_en_json_y_archivo_opcional() {
        assertEquals("${ASISTENCIAS_UCO_LOG_FILE:}", valor("logging.file.name"));
        assertEquals("${ASISTENCIAS_UCO_LOG_CONSOLE_FORMAT:logstash}", valor("logging.structured.format.console"));
    }

    @Test
    void cf_05_hsts_configurable_por_variable() {
        assertEquals("${APP_SECURITY_HSTS_ENABLED:true}", valor("app.security.headers.hsts-enabled"));
    }

    @Test
    void f12_09_dockerfile_declara_healthcheck_contra_la_sonda_de_readiness_del_puerto_de_gestion() throws IOException {
        final String dockerfile = Files.readString(Path.of("Dockerfile"), StandardCharsets.UTF_8);
        assertTrue(dockerfile.contains("HEALTHCHECK"), "el Dockerfile declara HEALTHCHECK");
        assertTrue(dockerfile.contains("http://127.0.0.1:${MANAGEMENT_SERVER_PORT:-8090}/actuator/health/readiness"));
        assertTrue(dockerfile.contains("ENTRYPOINT [\"java\""), "exec form: la JVM recibe SIGTERM");
    }

    @Test
    void f12_08_pool_de_conexiones_dimensionado_por_variable_y_por_replica() {
        assertEquals("${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:10}", valor("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:10}", valor("spring.datasource.hikari.minimum-idle"));
        assertEquals("${SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT:30000}", valor("spring.datasource.hikari.connection-timeout"));
    }

    @Test
    void rea_02_los_breakers_se_construyen_desde_application_yml() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CircuitBreakerAutoConfiguration.class))
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(yaml))
                .run(context -> {
                    final CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);
                    final CircuitBreakerConfig pep = registry
                            .circuitBreaker(ResilienceCircuitBreakerNames.SECURITY_BASELINE_PEP).getCircuitBreakerConfig();
                    assertEquals(50f, pep.getFailureRateThreshold());
                    assertEquals(20, pep.getSlidingWindowSize());
                    assertEquals(10, pep.getMinimumNumberOfCalls());
                    assertEquals(Duration.ofSeconds(2), pep.getSlowCallDurationThreshold());
                    assertEquals(Duration.ofSeconds(15).toMillis(),
                            pep.getWaitIntervalFunctionInOpenState().apply(1));

                    final CircuitBreakerConfig storage = registry
                            .circuitBreaker(ResilienceCircuitBreakerNames.FILE_STORAGE).getCircuitBreakerConfig();
                    assertEquals(Duration.ofSeconds(10), storage.getSlowCallDurationThreshold());
                    final CircuitBreakerConfig antivirus = registry
                            .circuitBreaker(ResilienceCircuitBreakerNames.MALWARE_SCAN).getCircuitBreakerConfig();
                    assertEquals(Duration.ofSeconds(30).toMillis(),
                            antivirus.getWaitIntervalFunctionInOpenState().apply(1));
                });
    }

    private static String valor(final String clave) {
        final Object valor = yaml.getProperty(clave);
        return valor == null ? null : valor.toString();
    }
}
