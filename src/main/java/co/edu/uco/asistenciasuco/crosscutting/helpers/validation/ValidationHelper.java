package co.edu.uco.asistenciasuco.crosscutting.helpers.validation;

import java.util.UUID;

public final class ValidationHelper {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private ValidationHelper() {
    }

    public static boolean hasText(final String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidPersonName(final String value) {
        if (!hasText(value)) {
            return true;
        }
        if (ValidationPatterns.CONTROL_WHITESPACE.matcher(value).find()) {
            return false;
        }
        return ValidationPatterns.PERSON_NAME.matcher(normalizeSpaces(value)).matches();
    }

    public static boolean isValidEmail(final String value) {
        if (!hasText(value)) {
            return true;
        }
        return ValidationPatterns.EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isNonEmptyUuid(final UUID value) {
        return value == null || !EMPTY_UUID.equals(value);
    }

    public static boolean isPositive(final Integer value) {
        return value == null || value > 0;
    }

    public static boolean hasDigitCount(final Integer value, final int min, final int max) {
        if (value == null || value <= 0) {
            return true;
        }
        final int digits = String.valueOf(value).length();
        return digits >= min && digits <= max;
    }

    public static boolean isLengthAtMost(final String value, final int max) {
        return value == null || value.length() <= max;
    }

    public static boolean isLengthBetween(final String value, final int min, final int max) {
        return value == null || value.length() >= min && value.length() <= max;
    }

    public static String normalizeSpaces(final String value) {
        if (value == null) {
            return null;
        }
        return trimRegularSpaces(ValidationPatterns.SPACE_SEQUENCE.matcher(value).replaceAll(" "));
    }

    private static String trimRegularSpaces(final String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(start, end);
    }
}
