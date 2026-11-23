package co.edu.uco.notificaciones.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Propaga {@code X-Correlation-Id}: si la peticion trae uno valido lo reutiliza, si no genera un UUID. Lo deja en el MDC
 * (los logs JSON lo incluyen como {@code correlationId}) y lo devuelve en la respuesta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Correlation-Id";
    public static final String MDC_CLAVE = "correlationId";
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        final String recibido = request.getHeader(CABECERA);
        final String id = recibido != null && VALIDO.matcher(recibido.trim()).matches() ? recibido.trim() : UUID.randomUUID().toString();
        MDC.put(MDC_CLAVE, id);
        response.setHeader(CABECERA, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CLAVE);
        }
    }
}
