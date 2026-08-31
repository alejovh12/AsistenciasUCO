package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.observability.tracing.TraceContextSnapshot;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public final class HttpRequestTraceFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestTraceFilter.class);
    private static final String EVENT_TYPE_HTTP_REQUEST_COMPLETED = "HTTP_REQUEST_COMPLETED";
    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_CLIENT_ERROR = "CLIENT_ERROR";
    private static final String RESULT_SERVER_ERROR = "SERVER_ERROR";

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final long startNanos = System.nanoTime();
        final TraceContextSnapshot initialTraceContext = refreshTraceContextMdc(TraceContextSnapshot.current());
        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } catch (Error exception) {
            failure = exception;
            throw exception;
        } finally {
            final TraceContextSnapshot finalTraceContext = mergeTraceContext(
                    initialTraceContext,
                    refreshTraceContextMdc(TraceContextSnapshot.current())
            );
            logRequestExit(request, response, finalTraceContext, startNanos, failure);
            TraceContextSnapshot.clearMdc();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private void logRequestExit(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final TraceContextSnapshot traceContext,
            final long startNanos,
            final Throwable failure
    ) {
        final long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        final int status = resolveStatus(response, failure);
        final String method = safe(request.getMethod(), 12);
        final String path = safe(request.getRequestURI(), 240);
        final String result = resultForStatus(status);
        LOGGER.atInfo()
                .addKeyValue("eventType", EVENT_TYPE_HTTP_REQUEST_COMPLETED)
                .addKeyValue("httpMethod", method)
                .addKeyValue("path", path)
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .addKeyValue("result", result)
                .log("HTTP request completed.");
    }

    private String resultForStatus(final int status) {
        if (status >= 500) {
            return RESULT_SERVER_ERROR;
        }
        if (status >= 400) {
            return RESULT_CLIENT_ERROR;
        }
        return RESULT_SUCCESS;
    }

    private int resolveStatus(final HttpServletResponse response, final Throwable failure) {
        final int currentStatus = response.getStatus();
        if (failure != null && currentStatus < 500) {
            return 500;
        }
        return currentStatus;
    }

    private TraceContextSnapshot refreshTraceContextMdc(final TraceContextSnapshot traceContext) {
        TraceContextSnapshot.clearMdc();
        traceContext.putInMdc();
        return traceContext;
    }

    private TraceContextSnapshot mergeTraceContext(
            final TraceContextSnapshot initialTraceContext,
            final TraceContextSnapshot finalTraceContext
    ) {
        final String traceId = finalTraceContext.traceId() != null ? finalTraceContext.traceId() : initialTraceContext.traceId();
        final String spanId = finalTraceContext.spanId() != null ? finalTraceContext.spanId() : initialTraceContext.spanId();
        return new TraceContextSnapshot(traceId, spanId);
    }

    private String safe(final String value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }
}
