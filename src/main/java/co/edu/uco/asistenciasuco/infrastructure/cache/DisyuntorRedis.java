package co.edu.uco.asistenciasuco.infrastructure.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Code review 2026-11-26 (REDIS-001): disyuntor (circuit breaker) compartido por los usos de Redis de una replica (cache
 * de consultas y limitador de intentos).
 *
 * <p>Tras una falla de Redis (caido, timeout) queda <b>abierto</b> durante {@code enfriamiento}: mientras tanto quienes lo
 * consultan no llaman a Redis y degradan de inmediato (BD o limitador local), en lugar de esperar el timeout de Redis en
 * cada peticion. Pasado el enfriamiento, la siguiente operacion vuelve a intentar (semiabierto): si funciona se cierra, si
 * falla se abre otra vez.</p>
 */
public final class DisyuntorRedis {

    /** Enfriamiento predeterminado tras una falla. */
    public static final Duration ENFRIAMIENTO = Duration.ofSeconds(30);

    private final Clock clock;
    private final Duration duracionEnfriamiento;
    private volatile Instant abiertoHasta = Instant.EPOCH;

    public DisyuntorRedis(final Clock clock) {
        this(clock, ENFRIAMIENTO);
    }

    public DisyuntorRedis(final Clock clock, final Duration enfriamiento) {
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.duracionEnfriamiento = Objects.requireNonNull(enfriamiento, "El enfriamiento es obligatorio.");
        if (enfriamiento.isNegative()) {
            throw new IllegalArgumentException("El enfriamiento del disyuntor no puede ser negativo.");
        }
    }

    /** {@code true} si se puede intentar una operacion contra Redis (cerrado o enfriamiento cumplido). */
    public boolean permite() {
        return !clock.instant().isBefore(abiertoHasta);
    }

    /** Registra una falla de Redis: abre el disyuntor durante el enfriamiento. */
    public void registrarFalla() {
        abiertoHasta = clock.instant().plus(duracionEnfriamiento);
    }

    /** Registra una operacion exitosa: cierra el disyuntor. */
    public void registrarExito() {
        if (!Instant.EPOCH.equals(abiertoHasta)) {
            abiertoHasta = Instant.EPOCH;
        }
    }
}
