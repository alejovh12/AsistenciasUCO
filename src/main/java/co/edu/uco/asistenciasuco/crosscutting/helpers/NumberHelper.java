package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;

/**
 * Utilidades para validacion de numeros.
 */
public final class NumberHelper {

    private NumberHelper() {
    }

    public static Integer requirePositive(final Integer value, final String nullMessage, final String notPositiveMessage) {
        ObjectHelper.requireNonNull(value, nullMessage);
        if (value <= 0) {
            throw new IllegalArgumentException(notPositiveMessage);
        }
        return value;
    }

    public static Integer requireDigitLengthBetween(
            final Integer value,
            final int minInclusive,
            final int maxInclusive,
            final String message
    ) {
        if (minInclusive < 0 || maxInclusive < minInclusive) {
            throw new CrosscuttingException("Rango de digitos invalido para la validacion numerica.");
        }

        ObjectHelper.requireNonNull(value, message);
        final int digits = String.valueOf(value).length();
        if (digits < minInclusive || digits > maxInclusive) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
