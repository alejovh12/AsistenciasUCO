package co.edu.uco.asistenciasuco.infrastructure.config.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Única definición de las rutas con {@code permitAll} de {@link SecurityConfig}. La reutiliza el
 * enforcement experimental de Security Baseline para excluirlas de forma explícita: todo lo demás
 * se considera protegido (deny-by-default), sin depender de prefijos de negocio.
 */
public final class SecurityPublicEndpoints {

    public static final String ACTUATOR = "/actuator/**";
    private static final String[] DOCUMENTATION_GET = {
            "/swagger-ui",
            "/swagger-ui/**",
            "/openapi/openapi-golden-path.yaml"
    };
    /** Vista de error del contenedor; no expone recursos de negocio. */
    public static final String ERROR_VIEW = "/error";

    private SecurityPublicEndpoints() {
    }

    /** Rutas GET de documentación runtime (copia defensiva). */
    public static String[] documentationGet() {
        return DOCUMENTATION_GET.clone();
    }

    /** Rutas que nunca deben requerir una decisión PEP. */
    public static RequestMatcher pepExempt() {
        final PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        final RequestMatcher[] documentation = new RequestMatcher[DOCUMENTATION_GET.length];
        for (int index = 0; index < DOCUMENTATION_GET.length; index++) {
            documentation[index] = paths.matcher(HttpMethod.GET, DOCUMENTATION_GET[index]);
        }
        return new OrRequestMatcher(
                paths.matcher(ACTUATOR),
                paths.matcher(AzureEventGridAuthFilter.WEBHOOK_PATH),
                paths.matcher(ERROR_VIEW),
                new OrRequestMatcher(documentation)
        );
    }
}
