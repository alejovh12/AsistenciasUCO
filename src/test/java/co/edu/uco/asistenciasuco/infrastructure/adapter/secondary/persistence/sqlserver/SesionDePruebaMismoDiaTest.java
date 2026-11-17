package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Ayudante de las fixtures de integracion frente a SES_028 (sesion en un solo dia de Bogota). */
class SesionDePruebaMismoDiaTest {

    private static final Duration DOS_HORAS = Duration.ofHours(2);

    @ParameterizedTest(name = "{0} UTC + 2 h no cruza la medianoche de Bogota")
    @CsvSource({
            "2026-11-16T00:43",   // 19:43 en Bogota (el CI que paso)
            "2026-11-16T02:59",   // 21:59 -> termina 23:59
            "2026-11-16T05:30",   // 00:30 del dia siguiente en Bogota
            "2026-11-16T13:00"    // 08:00 en Bogota
    })
    void conserva_el_inicio_cuando_la_sesion_termina_el_mismo_dia(final LocalDateTime inicioUtc) {
        assertEquals(inicioUtc, SesionDePruebaMismoDia.inicioSinCruzarMedianoche(inicioUtc, DOS_HORAS));
    }

    @ParameterizedTest(name = "{0} UTC + 2 h cruza y pasa a {1} UTC")
    @CsvSource({
            "2026-11-16T03:18, 2026-11-16T05:01",   // 22:18 en Bogota (hora del CI que fallo + 1 h)
            "2026-11-16T03:00, 2026-11-16T05:01",   // 22:00 -> terminaria justo a medianoche (otro dia)
            "2026-11-16T04:59, 2026-11-16T05:01"    // 23:59
    })
    void corre_la_sesion_al_dia_siguiente_cuando_cruzaria_la_medianoche(final LocalDateTime inicioUtc,
                                                                         final LocalDateTime esperadoUtc) {
        final LocalDateTime ajustado = SesionDePruebaMismoDia.inicioSinCruzarMedianoche(inicioUtc, DOS_HORAS);

        assertEquals(esperadoUtc, ajustado);
        assertEquals(
                ajustado.atZone(ZoneOffset.UTC).withZoneSameInstant(SesionDePruebaMismoDia.ZONA_INSTITUCIONAL).toLocalDate(),
                ajustado.plus(DOS_HORAS).atZone(ZoneOffset.UTC).withZoneSameInstant(SesionDePruebaMismoDia.ZONA_INSTITUCIONAL)
                        .toLocalDate(),
                "la sesion ajustada empieza y termina el mismo dia local");
    }

    @Test
    void rechaza_duraciones_que_no_caben_en_un_dia() {
        final var localDateTime2 = LocalDateTime.of(2026, 11, 16, 13, 0);
        final var duration2 = Duration.ofHours(24);
        assertThrows(IllegalArgumentException.class,
                () -> SesionDePruebaMismoDia.inicioSinCruzarMedianoche(localDateTime2, duration2));
        final var localDateTime = LocalDateTime.of(2026, 11, 16, 13, 0);
        final var duration = Duration.ofHours(-1);
        assertThrows(IllegalArgumentException.class,
                () -> SesionDePruebaMismoDia.inicioSinCruzarMedianoche(localDateTime, duration));
    }
}
