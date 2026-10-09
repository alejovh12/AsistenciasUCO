package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Inactive adapter-side UTC codec for the future approved v2 session contract.
 * DATETIME2(7) stores UTC clock values without an offset. Do NOT wire into v1.
 */
public final class HttpUtcInstantCodec {
    private static final Pattern STRICT_OFFSET_DATE_TIME = Pattern.compile(
            "[0-9]{4}-(?:[0-9]{2})-(?:[0-9]{2})T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{1,7})?(?:Z|[+-](?:[0-9]{2}):[0-9]{2})");
    private static final LocalDateTime MIN_SQL = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime MAX_SQL = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999900);

    private HttpUtcInstantCodec() { }

    public static LocalDateTime toUtcDatabaseDateTime(final String text) {
        if (text == null || !STRICT_OFFSET_DATE_TIME.matcher(text).matches()
                || text.endsWith("-00:00")) {
            throw invalidFormat();
        }
        try {
            final LocalDateTime utc = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            validateDatabasePrecision(utc);
            return utc;
        } catch (DateTimeParseException | java.time.DateTimeException exception) {
            throw invalidFormat();
        }
    }

    public static String fromUtcDatabaseDateTime(final LocalDateTime utcValue) {
        Objects.requireNonNull(utcValue, "UTC database value must not be null");
        validateDatabasePrecision(utcValue);
        return utcValue.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static void validateDatabasePrecision(final LocalDateTime utcValue) {
        if (utcValue.isBefore(MIN_SQL) || utcValue.isAfter(MAX_SQL)
                || utcValue.getNano() % 100 != 0) {
            throw new IllegalArgumentException("UTC value cannot be represented by SQL DATETIME2(7).");
        }
    }

    private static IllegalArgumentException invalidFormat() {
        return new IllegalArgumentException("ISO date-time with seconds, max 7 fractional digits and explicit offset required.");
    }
}
