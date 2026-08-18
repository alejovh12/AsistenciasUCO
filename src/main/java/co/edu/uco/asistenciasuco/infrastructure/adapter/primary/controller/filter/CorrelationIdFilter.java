package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public final class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = CorrelationIdContext.MDC_KEY;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final UUID correlationId = resolveCorrelationId(request);
        CorrelationIdContext.set(correlationId);
        response.setHeader(HEADER_NAME, correlationId.toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private UUID resolveCorrelationId(final HttpServletRequest request) {
        final String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            final UUID correlationId = UUID.fromString(headerValue.trim());
            return new UUID(0L, 0L).equals(correlationId) ? UUID.randomUUID() : correlationId;
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
