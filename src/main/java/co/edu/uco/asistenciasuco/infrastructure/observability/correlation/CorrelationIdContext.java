package co.edu.uco.asistenciasuco.infrastructure.observability.correlation;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIdContext {

    public static final String MDC_KEY = "correlationId";

    private static final ThreadLocal<String> CURRENT_ID = new ThreadLocal<>();

    private CorrelationIdContext() {
        throw new IllegalStateException("No es permitido instanciar el contexto de correlacion.");
    }

    public static void set(final UUID correlationId) {
        set(correlationId == null ? null : correlationId.toString());
    }

    public static void set(final String correlationId) {
        CURRENT_ID.set(correlationId);
        if (correlationId == null) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, correlationId);
    }

    public static UUID get() {
        final String correlationId = CURRENT_ID.get();
        return correlationId == null ? null : UUID.fromString(correlationId);
    }

    public static UUID require() {
        final UUID correlationId = get();
        if (correlationId == null) {
            throw new CrosscuttingException("No existe correlationId asociado al request actual.");
        }
        return correlationId;
    }

    public static String getAsString() {
        return CURRENT_ID.get();
    }

    public static void clear() {
        CURRENT_ID.remove();
        MDC.remove(MDC_KEY);
    }
}
