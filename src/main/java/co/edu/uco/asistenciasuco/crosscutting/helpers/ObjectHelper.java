package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;

import java.util.UUID;

/**
 * Utilidades para validación de objetos.
 */
public final class ObjectHelper {

    private ObjectHelper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static boolean isNull(final Object value) {
        return value == null;
    }

    public static boolean isNotNull(final Object value) {
        return !isNull(value);
    }

    public static <T> T requireNonNull(final T value, final String message) {
        if (isNull(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static UUID requireNotEmptyUuid(
            final UUID value,
            final String nullMessage,
            final String emptyMessage
    ) {
        requireNonNull(value, nullMessage);
        if (new UUID(0L, 0L).equals(value)) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return value;
    }
}
