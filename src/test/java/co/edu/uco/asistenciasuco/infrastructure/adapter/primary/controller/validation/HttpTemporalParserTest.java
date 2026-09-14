package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTemporalParserTest {

    @Test
    void parsesStrictIsoDatesTimesAndDateTimes() {
        assertEquals(LocalDate.of(2024, 2, 29), HttpTemporalParser.parseLocalDate(" 2024-02-29 ", "fecha"));
        assertEquals(LocalTime.of(8, 5), HttpTemporalParser.parseLocalTime("08:05", "hora"));
        assertEquals(LocalTime.of(8, 5, 9), HttpTemporalParser.parseLocalTime("08:05:09", "hora"));
        assertEquals(LocalDateTime.of(2024, 2, 29, 8, 5),
                HttpTemporalParser.parseLocalDateTime("2024-02-29T08:05", "inicio"));
        assertEquals(LocalDateTime.of(2024, 2, 29, 8, 5, 9),
                HttpTemporalParser.parseLocalDateTime("2024-02-29T08:05:09", "inicio"));
        assertNull(HttpTemporalParser.parseLocalDate(null, "fecha"));
        assertNull(HttpTemporalParser.parseLocalTime("  ", "hora"));
        assertNull(HttpTemporalParser.parseLocalDateTime("", "inicio"));
    }

    @Test
    void rejectsImpossibleDatesAndNonIsoOrZonedInputsWithFieldContext() {
        assertInvalid("2025-02-29", "fecha", () -> HttpTemporalParser.parseLocalDate("2025-02-29", "fecha"));
        assertInvalid("25:00", "hora", () -> HttpTemporalParser.parseLocalTime("25:00", "hora"));
        assertInvalid("2024-02-29T08:05Z", "inicio",
                () -> HttpTemporalParser.parseLocalDateTime("2024-02-29T08:05Z", "inicio"));
        final ValidationException invalid = assertThrows(ValidationException.class,
                () -> HttpTemporalParser.parseLocalDate("ayer", null));
        assertTrue(invalid.getMessage().contains("valor temporal"));
    }

    private static void assertInvalid(final String value, final String field,
                                      final org.junit.jupiter.api.function.Executable action) {
        final ValidationException exception = assertThrows(ValidationException.class, action, value);
        assertEquals("ERR_FECHA_HORA_INVALIDA", exception.getCode());
        assertTrue(exception.getMessage().contains(field));
    }
}
