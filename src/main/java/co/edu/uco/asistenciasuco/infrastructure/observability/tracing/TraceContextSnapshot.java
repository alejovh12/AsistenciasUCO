package co.edu.uco.asistenciasuco.infrastructure.observability.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;

public record TraceContextSnapshot(String traceId, String spanId) {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";

    public static TraceContextSnapshot current() {
        final SpanContext context = Span.current().getSpanContext();
        if (context == null || !context.isValid()) {
            return new TraceContextSnapshot(null, null);
        }
        return new TraceContextSnapshot(context.getTraceId(), context.getSpanId());
    }

    public void putInMdc() {
        putIfPresent(TRACE_ID_MDC_KEY, traceId);
        putIfPresent(SPAN_ID_MDC_KEY, spanId);
    }

    public static void clearMdc() {
        MDC.remove(TRACE_ID_MDC_KEY);
        MDC.remove(SPAN_ID_MDC_KEY);
    }

    private static void putIfPresent(final String key, final String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
