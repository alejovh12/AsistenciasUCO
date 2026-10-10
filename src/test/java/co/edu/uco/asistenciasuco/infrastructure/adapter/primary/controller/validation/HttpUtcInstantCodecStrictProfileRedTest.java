package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RED for the UTC-D02 wire profile (MAINT-003C), derived from the proposal text, not from the codec.
 *
 * <p>UTC-D02 admits RFC 3339 date-times whose offset is exactly {@code Z} or {@code ±HH:mm}.
 * {@code DateTimeFormatter.ISO_OFFSET_DATE_TIME} parses the offset leniently and accepts
 * {@code +02}, {@code +02:00:30}, a missing seconds field and nine fractional digits, which the
 * proposed OpenAPI pattern rejects and {@code dbo.Sesion.fechaHoraInicio datetime2(7)} cannot store
 * losslessly (UTC-D04: same instant, same SQL bytes). Expected today: the four rejection tests fail
 * because the codec accepts those inputs; the remaining tests characterise behaviour to keep.
 */
class HttpUtcInstantCodecStrictProfileRedTest {

    @Test
    void d02RejectsHourOnlyOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00+02"));
    }

    @Test
    void d02RejectsOffsetWithSeconds() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00+02:00:30"));
    }

    @Test
    void d02RejectsMissingSecondsBecauseRfc3339RequiresThem() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00+02:00"));
    }

    @Test
    void d02d04RejectsFractionalSecondsInsteadOfRoundingInDatetime2() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.123456789Z"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.5Z"));
    }

    @Test
    void d02AcceptsNegativeZeroOffsetAsTheSameUtcInstant() {
        // RFC 3339 section 4.3: -00:00 states the UTC instant is known; only the local offset is not.
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00-00:00"));
    }

    @Test
    void d02RejectsZoneRegionSuffixAndOutOfRangeFields() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00+02:00[Europe/Berlin]"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-02-30T10:00:00Z"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T24:00:00Z"));
    }

    @Test
    void d02RejectionMessageDoesNotEchoTheSuppliedValue() {
        final String supplied = "2026-07-15T16:00:00<script>";
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(supplied));
        assertFalse(error.getMessage().contains(supplied));
    }

    @Test
    void d04OutputAlwaysCarriesSecondsAndZuluSuffix() {
        assertEquals("2026-01-15T10:00:00Z",
                HttpUtcInstantCodec.fromUtcDatabaseDateTime(LocalDateTime.of(2026, 1, 15, 10, 0)));
        assertEquals("2026-10-25T00:30:00Z",
                HttpUtcInstantCodec.fromUtcDatabaseDateTime(LocalDateTime.of(2026, 10, 25, 0, 30)));
    }
}
