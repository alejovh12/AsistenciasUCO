package co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePdpProfileClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePdpAdminProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;

/** Composition Root del perfil de acceso en el PDP: solo existe en modo baseline. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.security", name = "provider", havingValue = "baseline")
@EnableConfigurationProperties(SecurityBaselinePdpAdminProperties.class)
public class SecurityBaselinePdpAdminConfiguration {

    @Bean
    public AccessProfilePort accessProfilePort(
            final SecurityBaselinePdpAdminProperties properties,
            final tools.jackson.databind.ObjectMapper objectMapper
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new SecurityBaselinePdpProfileClient(httpClient, objectMapper, properties, Clock.systemUTC());
    }
}
