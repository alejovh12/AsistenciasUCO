package co.edu.uco.asistenciasuco.infrastructure.config.docs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Publica exclusivamente la documentación runtime construida desde el contrato OpenAPI
 * canónico. No genera ni infiere contrato desde annotations Java.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "asistencias.swagger-ui", name = "enabled", havingValue = "true")
public class SwaggerUiConfiguration implements WebMvcConfigurer {

    private static final String SWAGGER_UI_WEBJAR_LOCATION =
            "classpath:/runtime-docs/swagger-ui/META-INF/resources/webjars/swagger-ui/5.32.15/";

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> openApiYamlMimeMapping() {
        return factory -> {
            final MimeMappings mappings = new MimeMappings();
            mappings.add("yaml", "application/yaml");
            factory.addMimeMappings(mappings);
        };
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/swagger-ui/", SWAGGER_UI_WEBJAR_LOCATION)
                .setCacheControl(CacheControl.noCache().mustRevalidate());

        registry.addResourceHandler("/openapi/**")
                .addResourceLocations("classpath:/runtime-docs/openapi/")
                .setCacheControl(CacheControl.noStore().mustRevalidate());
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui/index.html");
        registry.addRedirectViewController("/swagger-ui/", "/swagger-ui/index.html");
    }
}
