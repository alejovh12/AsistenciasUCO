package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure adapter-side codec for a future opt-in offset-aware HTTP session contract.
 *
 * <p>SQL Server Sesion.* stores UTC clock values in DATETIME2 (no offset).
 * The legacy v1 API accepts LocalDateTime without an offset; this class MUST
 * NOT be wired to v1 until a separately approved API contract handles migration.
 * Missing offsets are intentionally rejected rather than guessing the browser
 * or server time zone.
 *
 * <p>Wire profile UTC-D02 (MAINT-003D): {@code YYYY-MM-DDTHH:mm:ss[.1..7 digits](Z|±HH:mm)},
 * uppercase {@code T}/{@code Z}, no surrounding whitespace. The lexical pattern is checked
 * first and the strict ISO parser then validates the calendar date and the offset range;
 * the pattern alone is not sufficient. Seven fractional digits match DATETIME2(7), so values
 * are never rounded nor truncated.
 */
public final class HttpUtcInstantCodec {

    private static final Pattern D02_WIRE_PROFILE = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,7})?(?:Z|[+-]\\d{2}:\\d{2})$");

    private static final DateTimeFormatter STRICT_OFFSET_DATE_TIME =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);

    private HttpUtcInstantCodec() {
    }

    /** Convert an RFC3339 offset date-time (UTC-D02 profile) to UTC-local DATETIME2 values. */
    public static LocalDateTime toUtcDatabaseDateTime(final String offsetDateTime) {
        if (offsetDateTime == null || offsetDateTime.isBlank()) {
            throw new IllegalArgumentException("Se requiere fecha-hora ISO con offset.");
        }
        if (!D02_WIRE_PROFILE.matcher(offsetDateTime).matches()) {
            // Never echo user-supplied date strings or internals in a public error.
            throw new IllegalArgumentException("Fecha-hora invalida: requiere un offset Z o +/-HH:mm.");
        }
        try {
            return OffsetDateTime.parse(offsetDateTime, STRICT_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Fecha-hora invalida: requiere un offset Z o +/-HH:mm.", exception);
        }
    }

    /** Treat an already canonical DB UTC DATETIME2 as an instant (not local time). */
    public static String fromUtcDatabaseDateTime(final LocalDateTime databaseUtcValue) {
        Objects.requireNonNull(databaseUtcValue, "El valor de base de datos UTC es obligatorio.");
        return databaseUtcValue.atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
