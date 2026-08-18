package co.edu.uco.asistenciasuco.infrastructure.correlation;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIdContext {

    public static final String MDC_KEY = "correlationId";

    private static final ThreadLocal<UUID> CURRENT_ID = new ThreadLocal<>();

    private CorrelationIdContext() {
        throw new IllegalStateException("No es permitido instanciar el contexto de correlacion.");
    }

    public static void set(final UUID correlationId) {
        CURRENT_ID.set(correlationId);
        MDC.put(MDC_KEY, String.valueOf(correlationId));
    }

    public static UUID get() {
        return CURRENT_ID.get();
    }

    public static UUID require() {
        final UUID correlationId = get();
        if (correlationId == null) {
            throw new CrosscuttingException("No existe correlationId asociado al request actual.");
        }
        return correlationId;
    }

    public static String getAsString() {
        final UUID correlationId = get();
        return correlationId == null ? null : correlationId.toString();
    }

    public static void clear() {
        CURRENT_ID.remove();
        MDC.remove(MDC_KEY);
    }
}
