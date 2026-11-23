package co.edu.uco.notificaciones.infrastructure.web;

import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.ErrorResponse;
import co.edu.uco.notificaciones.infrastructure.config.NotificacionesProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Exige {@code X-Api-Key} en todo salvo las sondas de salud ({@code /actuator/health/**}) e {@code info}. La comparacion es
 * en tiempo constante (hash SHA-256 de ambas claves y {@link MessageDigest#isEqual}). El secreto viene de
 * {@code NOTIFICACIONES_API_KEY}; sin clave configurada el servicio no arranca.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Api-Key";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final byte[] huellaEsperada;
    private final JsonMapper json;

    public ApiKeyFilter(final NotificacionesProperties propiedades, final JsonMapper json) {
        final String clave = propiedades.apiKey();
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("NOTIFICACIONES_API_KEY es obligatoria: el servicio no arranca sin clave.");
        }
        this.huellaEsperada = sha256(clave);
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String ruta = request.getRequestURI();
        return ruta.equals("/actuator/health") || ruta.startsWith("/actuator/health/") || ruta.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        final String recibida = request.getHeader(CABECERA);
        if (recibida != null && MessageDigest.isEqual(huellaEsperada, sha256(recibida))) {
            chain.doFilter(request, response);
            return;
        }
        LOGGER.warn("Peticion rechazada por X-Api-Key ausente o invalida: metodo={}, ruta={}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(new ErrorResponse("NO_AUTORIZADO",
                "Falta la cabecera X-Api-Key o no es valida.", MDC.get(CorrelationFilter.MDC_CLAVE))));
    }

    private static byte[] sha256(final String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM.", ex);
        }
    }
}
