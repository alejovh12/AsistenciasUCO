package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Parser estricto para valores temporales recibidos por HTTP como String.
 */
public final class HttpTemporalParser {

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LOCAL_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("HH:mm")
                    .optionalStart()
                    .appendPattern(":ss")
                    .optionalEnd()
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral('T')
                    .append(LOCAL_TIME_FORMATTER)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);

    private HttpTemporalParser() {
    }

    public static LocalDate parseLocalDate(final String value, final String fieldName) {
        if (TextHelper.isNullOrBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), LOCAL_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw invalidTemporal(fieldName, "yyyy-MM-dd");
        }
    }

    public static LocalTime parseLocalTime(final String value, final String fieldName) {
        if (TextHelper.isNullOrBlank(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), LOCAL_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw invalidTemporal(fieldName, "HH:mm o HH:mm:ss");
        }
    }

    public static LocalDateTime parseLocalDateTime(final String value, final String fieldName) {
        if (TextHelper.isNullOrBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), LOCAL_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw invalidTemporal(fieldName, "yyyy-MM-dd'T'HH:mm o yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    private static ValidationException invalidTemporal(final String fieldName, final String expectedFormat) {
        final String normalizedField = TextHelper.isNullOrBlank(fieldName) ? "valor temporal" : fieldName;
        return new ValidationException(
                "ERR_FECHA_HORA_INVALIDA",
                "El campo " + normalizedField + " debe usar formato ISO local " + expectedFormat + "."
        );
    }
}
