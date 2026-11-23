package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.domain.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Un ciclo del despachador: reserva un lote con lease, entrega cada notificacion y registra el resultado. Si el lease del lote
 * esta por agotarse se corta el ciclo (otra instancia podria reservar lo que queda y duplicar el envio) y se liberan las
 * reservas no procesadas sin contar el intento.
 */
public class Despachador {

    private static final Logger LOGGER = LoggerFactory.getLogger(Despachador.class);
    private static final String METRICA = "notification.dispatch.total";
    private static final String OUTCOME = "outcome";
    private static final String MDC_CORRELACION = "correlationId";
    private static final Duration MARGEN_BASE = Duration.ofSeconds(5);

    private final NotificacionRepositoryPort repositorio;
    private final EnviadorCorreo enviador;
    private final ConfiguracionDespachador config;
    private final MetricasPort metricas;
    private final Clock clock;

    private Instant ultimaDepuracion = Instant.EPOCH;

    public Despachador(
            final NotificacionRepositoryPort repositorio,
            final EnviadorCorreo enviador,
            final ConfiguracionDespachador config,
            final MetricasPort metricas,
            final Clock clock
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "NotificacionRepositoryPort es obligatorio.");
        this.enviador = Objects.requireNonNull(enviador, "EnviadorCorreo es obligatorio.");
        this.config = Objects.requireNonNull(config, "ConfiguracionDespachador es obligatoria.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasPort es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    public ConfiguracionDespachador configuracion() {
        return config;
    }

    public int ejecutarCiclo() {
        return ejecutarCiclo(() -> true);
    }

    /** @return cuantas notificaciones se procesaron en el ciclo. */
    public int ejecutarCiclo(final BooleanSupplier continuar) {
        if (!enviador.disponible()) {
            return 0;
        }
        // Instante tomado ANTES de reservar: el vencimiento real se fija despues, asi que el calculo nunca sobreestima el lease.
        final Instant reservadoEn = clock.instant();
        final List<Notificacion> lote;
        try {
            lote = repositorio.reservar(config.batchSize(), config.lease(), config.instanceId(), reservadoEn);
        } catch (final RuntimeException ex) {
            LOGGER.warn("Fallo al reservar lote de notificaciones: {}", ex.getMessage());
            return 0;
        }
        if (lote == null || lote.isEmpty()) {
            return 0;
        }

        final Instant venceEn = reservadoEn.plus(config.lease());
        Duration maxEnvio = Duration.ZERO;
        int procesadas = 0;
        for (int i = 0; i < lote.size(); i++) {
            if (!continuar.getAsBoolean()) {
                liberarRestantes(lote.subList(i, lote.size()));
                break;
            }
            final Duration margen = margenSeguridad(maxEnvio);
            final Duration restante = Duration.between(clock.instant(), venceEn);
            if (restante.compareTo(margen) < 0) {
                final List<Notificacion> omitidas = lote.subList(i, lote.size());
                metricas.contar(METRICA, omitidas.size(), OUTCOME, "skipped_lease");
                LOGGER.warn("Lease del lote por agotarse (restante={}ms, margen={}ms): se omiten {} notificaciones reservadas; "
                                + "se reenviaran en el siguiente ciclo sin contar intento.",
                        restante.toMillis(), margen.toMillis(), omitidas.size());
                liberarRestantes(omitidas);
                break;
            }
            final long inicio = clock.millis();
            procesar(lote.get(i));
            procesadas++;
            final Duration duracion = Duration.ofMillis(Math.max(0, clock.millis() - inicio));
            if (duracion.compareTo(maxEnvio) > 0) {
                maxEnvio = duracion;
            }
        }
        return procesadas;
    }

    private void liberarRestantes(final List<Notificacion> restantes) {
        try {
            repositorio.liberar(restantes.stream().map(Notificacion::id).toList(), config.instanceId(), clock.instant());
        } catch (final RuntimeException ex) {
            // Si no se pueden liberar, el lease vence solo y se reintenta (contando el intento).
            LOGGER.warn("No fue posible liberar {} reservas: {}", restantes.size(), ex.getMessage());
        }
    }

    /** Margen minimo de lease antes de enviar: el mayor entre min(5 s, lease/4) y dos veces el envio mas lento del lote. */
    private Duration margenSeguridad(final Duration maxEnvio) {
        final Duration cuarto = config.lease().dividedBy(4);
        final Duration base = MARGEN_BASE.compareTo(cuarto) < 0 ? MARGEN_BASE : cuarto;
        final Duration adaptativo = maxEnvio.multipliedBy(2);
        return adaptativo.compareTo(base) > 0 ? adaptativo : base;
    }

    private void procesar(final Notificacion n) {
        final String anterior = MDC.get(MDC_CORRELACION);
        if (n.idCorrelacion() != null) {
            MDC.put(MDC_CORRELACION, n.idCorrelacion());
        }
        final long inicio = clock.millis();
        try {
            enviador.enviar(n);
            marcarEntregada(n, inicio);
        } catch (final NoEntregableException ex) {
            LOGGER.warn("Notificacion no entregable definitivamente id={}: {}", n.id(), ex.getMessage());
            metricas.contar(METRICA, 1, OUTCOME, "dead");
            registrarFallo(n, "FALLO_DEFINITIVO", ex.getMessage(), false);
        } catch (final RuntimeException ex) {
            LOGGER.warn("Fallo al despachar notificacion id={}: {}", n.id(), ex.getMessage());
            metricas.contar(METRICA, 1, OUTCOME, n.intentos() >= config.maxAttempts() ? "dead" : "retry");
            registrarFallo(n, "FALLO_REINTENTABLE", ex.getMessage(), true);
        } finally {
            if (anterior == null) {
                MDC.remove(MDC_CORRELACION);
            } else {
                MDC.put(MDC_CORRELACION, anterior);
            }
        }
    }

    private void marcarEntregada(final Notificacion n, final long inicio) {
        try {
            if (repositorio.marcarEntregada(n.id(), config.instanceId(), clock.instant())) {
                metricas.contar(METRICA, 1, OUTCOME, "delivered");
                metricas.medir("notification.dispatch.duration", Duration.ofMillis(clock.millis() - inicio).toNanos());
            } else {
                metricas.contar(METRICA, 1, OUTCOME, "lost_lease");
            }
        } catch (final RuntimeException ex) {
            LOGGER.warn("Fallo al marcar entregada notificacion id={}: {}", n.id(), ex.getMessage());
            metricas.contar(METRICA, 1, OUTCOME, "lost_lease");
        }
    }

    private void registrarFallo(final Notificacion n, final String codigo, final String detalle, final boolean reintentable) {
        try {
            final boolean registrado = repositorio.registrarFallo(n.id(), config.instanceId(), codigo, detalle, reintentable,
                    config.maxAttempts(), config.backoffBase(), config.backoffMax(), clock.instant());
            if (!registrado) {
                metricas.contar(METRICA, 1, OUTCOME, "lost_lease");
            }
        } catch (final RuntimeException ex) {
            LOGGER.warn("Fallo al registrar el fallo de la notificacion id={}: {}", n.id(), ex.getMessage());
            metricas.contar(METRICA, 1, OUTCOME, "lost_lease");
        }
    }

    /** Depura entregas terminales antiguas si ya paso el intervalo de retencion. */
    public void depurarSiCorresponde() {
        if (clock.instant().isAfter(ultimaDepuracion.plus(config.retentionInterval()))) {
            depurar();
        }
    }

    public int depurar() {
        try {
            final Instant limite = clock.instant().minus(Duration.ofDays(config.retentionDays()));
            final int eliminadas = repositorio.depurar(limite, config.batchSize() * 10);
            if (eliminadas > 0) {
                metricas.contar("notification.outbox.purged.total", eliminadas);
                LOGGER.info("Depuradas {} notificaciones terminales.", eliminadas);
            }
            ultimaDepuracion = clock.instant();
            return eliminadas;
        } catch (final RuntimeException ex) {
            LOGGER.warn("Fallo durante la depuracion de entregas: {}", ex.getMessage());
            return 0;
        }
    }
}
