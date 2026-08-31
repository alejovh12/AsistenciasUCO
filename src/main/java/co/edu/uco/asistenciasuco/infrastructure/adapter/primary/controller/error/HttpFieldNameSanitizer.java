package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import java.util.Optional;

public final class HttpFieldNameSanitizer {

    private static final int MAX_FIELD_LENGTH = 100;

    private HttpFieldNameSanitizer() {
    }

    public static Optional<String> sanitize(final String rawField) {
        if (rawField == null || rawField.isBlank()) {
            return Optional.empty();
        }

        final StringBuilder sanitized = new StringBuilder();
        int acceptedCharacters = 0;
        for (int offset = 0; offset < rawField.length() && acceptedCharacters < MAX_FIELD_LENGTH; ) {
            final int codePoint = rawField.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isISOControl(codePoint)) {
                continue;
            }
            sanitized.append(isAllowed(codePoint) ? Character.toString(codePoint) : "_");
            acceptedCharacters++;
        }

        if (sanitized.length() == 0 || sanitized.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(sanitized.toString());
    }

    private static boolean isAllowed(final int codePoint) {
        return Character.isLetter(codePoint)
                || Character.isDigit(codePoint)
                || codePoint == '_'
                || codePoint == '-'
                || codePoint == '.'
                || codePoint == '['
                || codePoint == ']';
    }
}
