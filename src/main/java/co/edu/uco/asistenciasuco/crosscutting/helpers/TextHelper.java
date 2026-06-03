package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;

import java.util.regex.Pattern;

/**
 * Utilidades para validación y normalización de texto.
 */
public final class TextHelper {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"
    );

    private TextHelper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static String trim(final String value) {
        return value == null ? null : value.trim();
    }

    public static boolean isNull(final String value) {
        return value == null;
    }

    public static boolean isBlank(final String value) {
        return !isNull(value) && trim(value).isEmpty();
    }

    public static boolean isNullOrBlank(final String value) {
        return isNull(value) || isBlank(value);
    }

    public static boolean hasLengthBetween(final String value, final int minInclusive, final int maxInclusive) {
        if (minInclusive < 0 || maxInclusive < minInclusive) {
            throw new CrosscuttingException("Rango de longitud inválido para la validación de texto.");
        }

        if (isNull(value)) {
            return false;
        }

        final int length = value.length();
        return length >= minInclusive && length <= maxInclusive;
    }

    public static boolean isEmailFormatValid(final String value) {
        if (isNullOrBlank(value)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trim(value)).matches();
    }
}
