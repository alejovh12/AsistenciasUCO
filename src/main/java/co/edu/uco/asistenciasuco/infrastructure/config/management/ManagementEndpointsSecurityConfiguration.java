package co.edu.uco.asistenciasuco.infrastructure.config.management;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * VAL H2 (OBS-08): cadena de seguridad propia del puerto de gestion de Actuator.
 *
 * <p>La cadena de la API ({@code SecurityConfig}) tambien cubre el contexto de gestion y exigia un Bearer para
 * {@code /actuator/prometheus}, asi que el scrape de Prometheus (Infra, {@code components/observability/prometheus/prometheus.yml},
 * {@code host.docker.internal:8090/actuator/prometheus}, sin credenciales) recibia 401. Esta cadena va primero y SOLO aplica a
 * peticiones que entraron por el puerto de gestion ({@link ManagementPortRequestMatcher}): ahi deja leer {@code health} y
 * {@code prometheus} con GET y niega todo lo demas. En el puerto de la API nada cambia: Prometheus no existe ahi y la ruta sigue
 * protegida por la cadena de la API.</p>
 *
 * <p>La proteccion del puerto de gestion es de red: escucha en loopback por defecto ({@code MANAGEMENT_SERVER_ADDRESS=127.0.0.1})
 * y nunca debe publicarse fuera de la maquina o de la red interna (ver {@code docs/observability/README.md}).</p>
 */
@Configuration(proxyBeanMethods = false)
public class ManagementEndpointsSecurityConfiguration {

    static final String[] ENDPOINTS_LECTURA = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus"
    };

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain managementPortSecurityFilterChain(
            final HttpSecurity http,
            final Environment environment
    ) throws Exception {
        http
            .securityMatcher(new ManagementPortRequestMatcher(environment))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(auth -> auth
                // El despacho a /error solo muestra el estado ya decidido (mismo criterio que la cadena de la API).
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, ENDPOINTS_LECTURA).permitAll()
                .anyRequest().denyAll()
            );
        return http.build();
    }
}
