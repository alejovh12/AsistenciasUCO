package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.UUID;

@Component
public final class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = CorrelationIdContext.MDC_KEY;
    private static final UUID ZERO_UUID = new UUID(0L, 0L);
    private static final Pattern CANONICAL_UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final String correlationId = resolveCorrelationId(request);
        CorrelationIdContext.set(correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private String resolveCorrelationId(final HttpServletRequest request) {
        final String headerValue = request.getHeader(HEADER_NAME);
        if (!isAcceptedCorrelationId(headerValue)) {
            return UUID.randomUUID().toString();
        }
        final UUID correlationId = UUID.fromString(headerValue);
        return ZERO_UUID.equals(correlationId) ? UUID.randomUUID().toString() : headerValue;
    }

    private boolean isAcceptedCorrelationId(final String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }
        if (!headerValue.equals(headerValue.trim())) {
            return false;
        }
        if (!CANONICAL_UUID_PATTERN.matcher(headerValue).matches()) {
            return false;
        }
        return !headerValue.toLowerCase(Locale.ROOT).contains("%0a")
                && !headerValue.toLowerCase(Locale.ROOT).contains("%0d");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
