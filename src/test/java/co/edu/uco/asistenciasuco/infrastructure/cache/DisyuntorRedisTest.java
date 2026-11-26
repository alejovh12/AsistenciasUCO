package co.edu.uco.asistenciasuco.infrastructure.cache;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Code review 2026-11-26: el disyuntor evita esperar el timeout de Redis en cada peticion mientras esta caido. */
class DisyuntorRedisTest {

    private static final class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-11-26T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Test
    void se_abre_tras_una_falla_y_permite_reintentar_al_cumplir_el_enfriamiento() {
        final Reloj reloj = new Reloj();
        final DisyuntorRedis disyuntor = new DisyuntorRedis(reloj, Duration.ofSeconds(30));
        assertTrue(disyuntor.permite());

        disyuntor.registrarFalla();
        assertFalse(disyuntor.permite());
        reloj.ahora = reloj.ahora.plusSeconds(29);
        assertFalse(disyuntor.permite());
        reloj.ahora = reloj.ahora.plusSeconds(1);
        assertTrue(disyuntor.permite(), "semiabierto: se puede intentar otra vez");

        disyuntor.registrarFalla();
        assertFalse(disyuntor.permite(), "si el reintento falla se vuelve a abrir");
        disyuntor.registrarExito();
        assertTrue(disyuntor.permite(), "un exito lo cierra");
    }

    @Test
    void el_enfriamiento_no_puede_ser_negativo() {
        final Clock reloj = Clock.systemUTC();
        final Duration negativo = Duration.ofSeconds(-1);
        assertThrows(IllegalArgumentException.class, () -> new DisyuntorRedis(reloj, negativo));
        assertTrue(new DisyuntorRedis(reloj).permite());
    }
}
