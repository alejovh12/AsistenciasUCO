package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;

import java.util.Locale;
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

    public static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalizeTrimUpper(final String value) {
        final String trimmed = trim(value);
        return isNull(trimmed) ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    public static String normalizeTrimUpperToEmpty(final String value) {
        return trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    public static String normalizeTrimLower(final String value) {
        final String trimmed = trim(value);
        return isNull(trimmed) ? null : trimmed.toLowerCase(Locale.ROOT);
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

    public static String requireNonBlank(final String value, final String message) {
        if (isNullOrBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String requireMaxLength(final String value, final int maxInclusive, final String message) {
        if (maxInclusive < 0) {
            throw new CrosscuttingException("Longitud maxima invalida para la validacion de texto.");
        }
        if (isNotNull(value) && value.length() > maxInclusive) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String requireLengthBetween(
            final String value,
            final int minInclusive,
            final int maxInclusive,
            final String message
    ) {
        if (!hasLengthBetween(value, minInclusive, maxInclusive)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String requireEmailFormat(final String value, final String message) {
        if (isNull(value) || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static boolean containsWhitespace(final String value) {
        return isNotNull(value) && value.chars().anyMatch(Character::isWhitespace);
    }

    public static boolean isNotNull(final String value) {
        return !isNull(value);
    }
}
