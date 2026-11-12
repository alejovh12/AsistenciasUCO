package co.edu.uco.asistenciasuco.infrastructure.config.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * CF-05: encabezados de seguridad explicitos del backend (no se depende de los valores por defecto de Spring Security).
 *
 * <ul>
 *   <li>{@code Content-Security-Policy}: la API solo devuelve JSON, asi que no carga nada ({@code default-src 'none'}) y
 *       no se puede enmarcar. Swagger UI (solo si {@code asistencias.swagger-ui.enabled=true}) recibe una politica
 *       limitada a su propio origen: sus scripts son archivos estaticos y solo necesita estilos en linea e imagenes
 *       {@code data:}.</li>
 *   <li>{@code X-Frame-Options: DENY} y {@code frame-ancestors 'none'} (clickjacking).</li>
 *   <li>{@code Strict-Transport-Security} condicionado: Spring Security solo lo escribe en peticiones HTTPS (detras del
 *       proxy, con los encabezados reenviados); se puede apagar con {@code APP_SECURITY_HSTS_ENABLED=false}.</li>
 *   <li>{@code X-Content-Type-Options: nosniff}, {@code Referrer-Policy: no-referrer} y un {@code Permissions-Policy}
 *       que niega camara, microfono, geolocalizacion y pagos.</li>
 * </ul>
 */
final class SecurityHttpHeaders {

    static final String API_CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    static final String DOCS_CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; frame-ancestors 'none'; "
            + "base-uri 'self'; form-action 'self'";
    static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";
    static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    private SecurityHttpHeaders() {
    }

    static void apply(final HeadersConfigurer<HttpSecurity> headers, final boolean hstsEnabled) {
        final RequestMatcher docs = new OrRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui"),
                PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
                PathPatternRequestMatcher.withDefaults().matcher("/openapi/**")
        );
        headers
                .contentTypeOptions(contentTypeOptions -> { })
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(docs,
                        new StaticHeadersWriter("Content-Security-Policy", DOCS_CSP)))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(docs),
                        new StaticHeadersWriter("Content-Security-Policy", API_CSP)))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY));
        if (hstsEnabled) {
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS));
        } else {
            headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
        }
    }
}
