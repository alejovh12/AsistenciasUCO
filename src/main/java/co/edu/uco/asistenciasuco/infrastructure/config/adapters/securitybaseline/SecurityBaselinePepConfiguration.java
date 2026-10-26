package co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPepEnforcementFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/** Composition Root aislado del enforcement Security Baseline 0.1.7. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.security", name = "provider", havingValue = "baseline")
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
                // "Upgrade: h2c" y el PEP local puede responder 501.
                .version(HttpClient.Version.HTTP_1_1)
                // Destino fijo: nunca seguir un redirect del PEP (fail-closed como respuesta anómala).
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new SecurityBaselinePepClient(httpClient, objectMapper, properties);
    }

    @Bean
    public SecurityPepEnforcementFilter securityBaselinePepFilter(
            final SecurityBaselinePepClient client,
            final SecurityErrorResponseWriter responseWriter
    ) {
        return new SecurityPepEnforcementFilter(client, responseWriter);
    }

    /**
     * Spring Boot registra como filtro servlet global cualquier bean {@code Filter}. Este filtro solo
     * debe vivir dentro de la {@code SecurityFilterChain} (donde existe el {@code SecurityContext}),
     * así que su registro servlet se deshabilita explícitamente.
     */
    @Bean
    public FilterRegistrationBean<SecurityPepEnforcementFilter> securityBaselinePepFilterServletRegistration(
            final SecurityPepEnforcementFilter filter
    ) {
        final FilterRegistrationBean<SecurityPepEnforcementFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
