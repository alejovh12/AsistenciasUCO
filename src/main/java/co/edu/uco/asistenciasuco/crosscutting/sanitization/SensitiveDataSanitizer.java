package co.edu.uco.asistenciasuco.crosscutting.sanitization;

import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    public static final String REDACTED = "[REDACTED]";
    private static final int DEFAULT_MAX_LENGTH = 512;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}&&[^\t]]");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)(authorization|password|passwd|pwd|token|secret|cookie|credential|api[-_]?key)(\\s*[:=]\\s*)([^,;}]+)"
    );
    private static final Pattern SENSITIVE_WORD = Pattern.compile(
            "(?i)\\b(authorization|password|passwd|pwd|access[-_]?token|refresh[-_]?token|token|secret|cookie|credential|api[-_]?key)\\b"
    );

    private SensitiveDataSanitizer() {
        throw new IllegalStateException("No es permitido instanciar un sanitizador de datos sensibles.");
    }

    public static String sanitizeForLog(final Object value) {
        return sanitizeForLog(value, DEFAULT_MAX_LENGTH);
    }

    public static String sanitizeForLog(final Object value, final int maxLength) {
        if (value == null) {
            return null;
        }
        return truncate(redactSecrets(removeControlCharacters(String.valueOf(value))), maxLength);
    }

    public static String sanitizePublicMessage(final String message, final String fallback) {
        if (TextHelper.isNullOrBlank(message)) {
            return fallback;
        }
        final String sanitized = sanitizeForLog(message, 240);
        return TextHelper.isNullOrBlank(sanitized) ? fallback : sanitized;
    }

    public static Map<String, String> sanitizeMetadata(final Map<String, ?> metadata) {
        final Map<String, String> sanitized = new LinkedHashMap<>();
        if (metadata == null || metadata.isEmpty()) {
            return sanitized;
        }
        metadata.forEach((key, value) -> {
            final boolean sensitiveKey = isSensitiveKey(key);
            final String safeKey = sensitiveKey ? REDACTED : sanitizeForLog(key, 80);
            if (TextHelper.isNullOrBlank(safeKey)) {
                return;
            }
            sanitized.put(safeKey, sensitiveKey ? REDACTED : sanitizeForLog(value, 240));
        });
        return sanitized;
    }

    public static boolean isSensitiveKey(final String key) {
        if (TextHelper.isNullOrBlank(key)) {
            return false;
        }
        final String normalizedKey = key.toLowerCase(Locale.ROOT);
        return normalizedKey.contains("authorization")
                || normalizedKey.contains("password")
                || normalizedKey.contains("passwd")
                || normalizedKey.contains("token")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("cookie")
                || normalizedKey.contains("credential")
                || normalizedKey.contains("api-key")
                || normalizedKey.contains("apikey");
    }

    private static String removeControlCharacters(final String value) {
        return CONTROL_CHARACTERS.matcher(value)
                .replaceAll("_")
                .replace('\n', '_')
                .replace('\r', '_');
    }

    private static String redactSecrets(final String value) {
        final String withoutAssignments = SENSITIVE_ASSIGNMENT.matcher(value).replaceAll("$1$2" + REDACTED);
        return SENSITIVE_WORD.matcher(withoutAssignments).replaceAll(REDACTED);
    }

    private static String truncate(final String value, final int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return ".".repeat(maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
