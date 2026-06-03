package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;

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
}
