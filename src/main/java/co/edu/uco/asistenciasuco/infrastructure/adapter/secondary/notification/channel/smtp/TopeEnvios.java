package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Controlador de tope de envios de correo por minuto y por dia (CONTRACT §4.3, D10).
 * Thread-safe y gobernado por {@link Clock} inyectable para determinismo en pruebas.
 */
public class TopeEnvios {

    private static final long SEGUNDOS_POR_MINUTO = 60L;
    private static final long SEGUNDOS_POR_DIA = 86_400L;

    private final int ratePerMinute;
    private final int ratePerDay;
    private final Clock clock;
    private final Counter throttledMinuteCounter;
    private final Counter throttledDayCounter;

    private long minutoActual = -1L;
    private long diaActual = -1L;
    private int usadosEnMinuto = 0;
    private int usadosEnDia = 0;

    public TopeEnvios(
            final int ratePerMinute,
            final int ratePerDay,
            final Clock clock,
            final MeterRegistry meterRegistry
    ) {
        if (ratePerMinute < 1) {
            throw new IllegalArgumentException("ratePerMinute debe ser >= 1.");
        }
        if (ratePerDay < ratePerMinute) {
            throw new IllegalArgumentException("ratePerDay debe ser >= ratePerMinute.");
        }
        this.ratePerMinute = ratePerMinute;
        this.ratePerDay = ratePerDay;
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");

        this.throttledMinuteCounter = Counter.builder("notification.email.throttled.total")
                .tag("window", "minute")
                .register(meterRegistry);
        this.throttledDayCounter = Counter.builder("notification.email.throttled.total")
                .tag("window", "day")
                .register(meterRegistry);
    }

    /**
     * Consulta sin consumir cupo si ambas ventanas (minuto y dia) tienen disponibilidad.
     */
    public synchronized boolean disponible() {
        actualizarVentanas(clock.instant());
        return usadosEnDia < ratePerDay && usadosEnMinuto < ratePerMinute;
    }

    /**
     * Intenta consumir 1 envio del cupo por minuto y diario.
     *
     * @return {@code true} si se consumio el cupo; {@code false} si alguna ventana esta agotada
     */
    public synchronized boolean intentarConsumir() {
        actualizarVentanas(clock.instant());
        if (usadosEnDia >= ratePerDay) {
            throttledDayCounter.increment();
            return false;
        }
        if (usadosEnMinuto >= ratePerMinute) {
            throttledMinuteCounter.increment();
            return false;
        }
        usadosEnMinuto++;
        usadosEnDia++;
        return true;
    }

    /**
     * Reinicia los contadores de ventana actual.
     */
    public synchronized void reiniciarVentanas() {
        this.minutoActual = -1L;
        this.diaActual = -1L;
        this.usadosEnMinuto = 0;
        this.usadosEnDia = 0;
    }

    private void actualizarVentanas(final Instant ahora) {
        final long epochSecond = ahora.getEpochSecond();
        final long nuevoMinuto = epochSecond / SEGUNDOS_POR_MINUTO;
        final long nuevoDia = epochSecond / SEGUNDOS_POR_DIA;

        if (nuevoDia != diaActual) {
            diaActual = nuevoDia;
            usadosEnDia = 0;
        }
        if (nuevoMinuto != minutoActual) {
            minutoActual = nuevoMinuto;
            usadosEnMinuto = 0;
        }
    }
}
