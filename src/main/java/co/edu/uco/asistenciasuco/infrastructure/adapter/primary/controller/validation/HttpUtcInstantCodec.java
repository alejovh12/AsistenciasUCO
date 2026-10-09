package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Pure adapter-side codec for a future opt-in offset-aware HTTP session contract.
 *
 * <p>SQL Server Sesion.* stores UTC clock values in DATETIME2 (no offset).
 * The legacy v1 API accepts LocalDateTime without an offset; this class MUST
 * NOT be wired to v1 until a separately approved API contract handles migration.
 * Missing offsets are intentionally rejected rather than guessing the browser
 * or server time zone.
 */
public final class HttpUtcInstantCodec {

    private HttpUtcInstantCodec() {
    }

    /** Convert RFC3339 / ISO offset date-time to UTC-local DATETIME2 values. */
    public static LocalDateTime toUtcDatabaseDateTime(final String offsetDateTime) {
        if (offsetDateTime == null || offsetDateTime.isBlank()) {
            throw new IllegalArgumentException("Se requiere fecha-hora ISO con offset.");
        }
        try {
            return OffsetDateTime.parse(offsetDateTime.trim(),
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException exception) {
            // Never echo user-supplied date strings or internals in a public error.
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
