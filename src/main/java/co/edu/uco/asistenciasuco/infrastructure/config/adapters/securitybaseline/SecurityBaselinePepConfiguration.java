package co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityBaselinePepFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/** Composition Root aislado del enforcement experimental Security Baseline. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.providers.security-baseline-pep",
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(SecurityBaselinePepProperties.class)
public class SecurityBaselinePepConfiguration {

    @Bean
    public SecurityBaselinePepClient securityBaselinePepClient(
            final SecurityBaselinePepProperties properties,
            final tools.jackson.databind.ObjectMapper objectMapper
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                // HTTP/1.1 explicito: con HTTP_2 (default del JDK) en texto plano se envia
                // "Upgrade: h2c" y el PEP v0.1.6 (Reactor Netty) responde 501, comprobado en el laboratorio.
                .version(HttpClient.Version.HTTP_1_1)
                // Destino fijo: nunca seguir un redirect del PEP (fail-closed como respuesta anómala).
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new SecurityBaselinePepClient(httpClient, objectMapper, properties);
    }

    @Bean
    public SecurityBaselinePepFilter securityBaselinePepFilter(
            final SecurityBaselinePepClient client,
            final SecurityBaselinePepProperties properties,
            final SecurityErrorResponseWriter responseWriter
    ) {
        return new SecurityBaselinePepFilter(client, properties, responseWriter);
    }

    /**
     * Spring Boot registra como filtro servlet global cualquier bean {@code Filter}. Este filtro solo
     * debe vivir dentro de la {@code SecurityFilterChain} (donde existe el {@code SecurityContext}),
     * así que su registro servlet se deshabilita explícitamente.
     */
    @Bean
    public FilterRegistrationBean<SecurityBaselinePepFilter> securityBaselinePepFilterServletRegistration(
            final SecurityBaselinePepFilter filter
    ) {
        final FilterRegistrationBean<SecurityBaselinePepFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
