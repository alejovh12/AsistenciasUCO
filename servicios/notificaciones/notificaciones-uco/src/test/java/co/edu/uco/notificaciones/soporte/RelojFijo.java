package co.edu.uco.notificaciones.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj mutable para pruebas deterministas. */
public class RelojFijo extends Clock {

    private Instant instante;

    public RelojFijo(final Instant inicial) {
        this.instante = inicial;
    }

    public void avanzar(final Duration d) {
        instante = instante.plus(d);
    }

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
        return instante;
    }
}
