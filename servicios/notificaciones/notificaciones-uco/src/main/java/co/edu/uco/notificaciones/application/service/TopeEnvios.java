package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.port.out.MetricasPort;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Tope de envios de correo por minuto y por dia. Thread-safe; el reloj es inyectable para pruebas deterministas. */
public class TopeEnvios {

    private static final long SEGUNDOS_POR_MINUTO = 60L;
    private static final long SEGUNDOS_POR_DIA = 86_400L;
    private static final String METRICA = "notification.email.throttled.total";

    private final int ratePerMinute;
    private final int ratePerDay;
    private final Clock clock;
    private final MetricasPort metricas;

    private long minutoActual = -1L;
    private long diaActual = -1L;
    private int usadosEnMinuto;
    private int usadosEnDia;

    public TopeEnvios(final int ratePerMinute, final int ratePerDay, final Clock clock, final MetricasPort metricas) {
        if (ratePerMinute < 1) {
            throw new IllegalArgumentException("ratePerMinute debe ser >= 1.");
        }
        if (ratePerDay < ratePerMinute) {
            throw new IllegalArgumentException("ratePerDay debe ser >= ratePerMinute.");
        }
        this.ratePerMinute = ratePerMinute;
        this.ratePerDay = ratePerDay;
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasPort es obligatorio.");
    }

    /** Consulta sin consumir cupo si ambas ventanas tienen disponibilidad. */
    public synchronized boolean disponible() {
        actualizarVentanas(clock.instant());
        return usadosEnDia < ratePerDay && usadosEnMinuto < ratePerMinute;
    }

    /** Consume un envio del cupo por minuto y diario; false si alguna ventana esta agotada. */
    public synchronized boolean intentarConsumir() {
        actualizarVentanas(clock.instant());
        if (usadosEnDia >= ratePerDay) {
            metricas.contar(METRICA, 1, "window", "day");
            return false;
        }
        if (usadosEnMinuto >= ratePerMinute) {
            metricas.contar(METRICA, 1, "window", "minute");
            return false;
        }
        usadosEnMinuto++;
        usadosEnDia++;
        return true;
    }

    public synchronized void reiniciarVentanas() {
        minutoActual = -1L;
        diaActual = -1L;
        usadosEnMinuto = 0;
        usadosEnDia = 0;
    }

    private void actualizarVentanas(final Instant ahora) {
        final long segundos = ahora.getEpochSecond();
        final long nuevoMinuto = segundos / SEGUNDOS_POR_MINUTO;
        final long nuevoDia = segundos / SEGUNDOS_POR_DIA;
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
