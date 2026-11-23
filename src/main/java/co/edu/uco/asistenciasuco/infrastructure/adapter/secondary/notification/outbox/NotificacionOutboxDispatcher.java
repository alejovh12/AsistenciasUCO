package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Despachador de notificaciones outbox basado en ciclo de vida SmartLifecycle.
 * Ejecuta un trabajador en segundo plano con hilo propio para reservar, despachar y depurar.
 */
public class NotificacionOutboxDispatcher implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionOutboxDispatcher.class);
    private static final String TAG_OUTCOME = "outcome";
    private static final String METRIC_DISPATCH_TOTAL = "notification.dispatch.total";

    private final NotificacionOutboxRepository repository;
    private final NotificacionChannelSender sender;
    private final OutboxNotificationProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object monitor = new Object();
    private Thread workerThread;

    private final Counter deliveredCounter;
    private final Counter retryCounter;
    private final Counter deadCounter;
    private final Counter lostLeaseCounter;
    private final Counter skippedLeaseCounter;
    private final Counter purgedCounter;
    private final Timer dispatchTimer;

    private Instant ultimaDepuracion = Instant.EPOCH;

    public NotificacionOutboxDispatcher(
            final NotificacionOutboxRepository repository,
            final NotificacionChannelSender sender,
            final OutboxNotificationProperties properties,
            final MeterRegistry meterRegistry,
            final Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "NotificacionOutboxRepository es obligatorio.");
        this.sender = Objects.requireNonNull(sender, "NotificacionChannelSender es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "OutboxNotificationProperties es obligatorio.");
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.objectMapper = new ObjectMapper();

        this.deliveredCounter = Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag(TAG_OUTCOME, "delivered")
                .register(meterRegistry);
        this.retryCounter = Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag(TAG_OUTCOME, "retry")
                .register(meterRegistry);
        this.deadCounter = Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag(TAG_OUTCOME, "dead")
                .register(meterRegistry);
        this.lostLeaseCounter = Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag(TAG_OUTCOME, "lost_lease")
                .register(meterRegistry);
        this.skippedLeaseCounter = Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag(TAG_OUTCOME, "skipped_lease")
                .register(meterRegistry);
        this.purgedCounter = Counter.builder("notification.outbox.purged.total")
                .register(meterRegistry);
        this.dispatchTimer = Timer.builder("notification.dispatch.duration")
                .register(meterRegistry);
    }

    public void signal() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public void despertar() {
        signal();
    }

    public String instanceId() {
        return properties.instanceId();
    }

    @Override
    public void start() {
        if (!properties.dispatcherEnabled()) {
            LOGGER.info("Despachador outbox deshabilitado por configuracion (dispatcherEnabled=false).");
            return;
        }

        if (running.compareAndSet(false, true)) {
            ejecutarDepuracion();
            workerThread = new Thread(this::runLoop, "notificacion-outbox-dispatcher-" + instanceId());
            workerThread.setDaemon(true);
            workerThread.start();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Despachador outbox iniciado para instancia: {}", instanceId());
            }
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
            if (workerThread != null) {
                try {
                    workerThread.join(TimeUnit.SECONDS.toMillis(5));
                } catch (final InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
            LOGGER.info("Despachador outbox detenido.");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void runLoop() {
        while (running.get()) {
            try {
                ejecutarCiclo();
                verificarDepuracionPeriodica();
            } catch (final Exception e) {
                LOGGER.error("Error inesperado en ciclo del despachador outbox: {}", e.getMessage(), e);
            }

            synchronized (monitor) {
                if (running.get()) {
                    try {
                        monitor.wait(properties.sweeperInterval().toMillis());
                    } catch (final InterruptedException _) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    public void ejecutarCiclo() {
        if (!sender.disponible()) {
            return;
        }
        final boolean establishedCorrelation = CorrelationIdContext.get() == null;
        if (establishedCorrelation) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            List<NotificacionReservada> lote;
            // Instante tomado ANTES de reservar: el SP fija el vencimiento despues, por lo que
            // este calculo es conservador (nunca sobreestima el lease restante).
            final Instant reservadoEn = clock.instant();
            try {
                lote = repository.reservar(properties.batchSize(), properties.lease(), instanceId());
            } catch (final Exception e) {
                LOGGER.warn("Fallo al reservar lote de notificaciones outbox: {}", e.getMessage());
                return;
            }

            if (lote == null || lote.isEmpty()) {
                return;
            }

            final Instant venceEn = reservadoEn.plus(properties.lease());
            Duration maxEnvio = Duration.ZERO;
            for (int i = 0; i < lote.size(); i++) {
                if (!running.get() && workerThread != null && Thread.currentThread() == workerThread) {
                    break;
                }
                final Duration margen = margenSeguridad(maxEnvio);
                final Duration restante = Duration.between(clock.instant(), venceEn);
                if (restante.compareTo(margen) < 0) {
                    final int omitidas = lote.size() - i;
                    skippedLeaseCounter.increment(omitidas);
                    LOGGER.warn("Lease del lote por agotarse (restante={}ms, margen={}ms): se omiten {} notificaciones "
                                    + "reservadas; se reenviaran en el siguiente ciclo sin contar intento.",
                            restante.toMillis(), margen.toMillis(), omitidas);
                    break;
                }
                final long t0 = clock.millis();
                procesarNotificacion(lote.get(i));
                final Duration duracion = Duration.ofMillis(Math.max(0, clock.millis() - t0));
                if (duracion.compareTo(maxEnvio) > 0) {
                    maxEnvio = duracion;
                }
            }
        } finally {
            if (establishedCorrelation) {
                CorrelationIdContext.clear();
            }
        }
    }

    /**
     * Margen minimo de lease que debe quedar antes de enviar: el mayor entre 5 s (acotado a lease/4)
     * y dos veces el envio mas lento observado en el lote, para que envio + marcarEntregada quepan.
     */
    private Duration margenSeguridad(final Duration maxEnvio) {
        final Duration base = Duration.ofSeconds(5).compareTo(properties.lease().dividedBy(4)) < 0
                ? Duration.ofSeconds(5)
                : properties.lease().dividedBy(4);
        final Duration adaptativo = maxEnvio.multipliedBy(2);
        return adaptativo.compareTo(base) > 0 ? adaptativo : base;
    }

    private void procesarNotificacion(final NotificacionReservada reservada) {
        final List<DestinatarioRef> destinatarios = deserializarDestinatarios(reservada.destinatariosJson());
        final Map<String, String> datos = deserializarDatos(reservada.datosJson());

        final NotificacionPersistida persistida = new NotificacionPersistida(
                reservada.id(),
                reservada.tipo(),
                reservada.asunto(),
                reservada.mensaje(),
                destinatarios,
                datos,
                reservada.intentos(),
                reservada.creadoEn()
        );

        final long inicio = clock.millis();
        try {
            sender.enviar(persistida);
            try {
                repository.marcarEntregada(reservada.id(), instanceId());
                deliveredCounter.increment();
                dispatchTimer.record(clock.millis() - inicio, TimeUnit.MILLISECONDS);
            } catch (final Exception ex) {
                LOGGER.warn("Reserva perdida o fallo al marcar entregada notificacion id={}: {}", reservada.id(), ex.getMessage());
                lostLeaseCounter.increment();
            }
        } catch (final NotificacionNoEntregableException ex) {
            LOGGER.warn("Notificacion no entregable definitivamente id={}: {}", reservada.id(), ex.getMessage());
            deadCounter.increment();
            try {
                repository.registrarFallo(
                        reservada.id(),
                        instanceId(),
                        "FALLO_DEFINITIVO",
                        ex.getMessage(),
                        false,
                        properties.maxAttempts(),
                        properties.backoffBase(),
                        properties.backoffMax()
                );
            } catch (final Exception regEx) {
                LOGGER.warn("Fallo al registrar fallo definitivo en DB para id={}: {}", reservada.id(), regEx.getMessage());
                lostLeaseCounter.increment();
            }
        } catch (final Exception ex) {
            LOGGER.warn("Fallo al despachar notificacion id={}: {}", reservada.id(), ex.getMessage());
            if (reservada.intentos() >= properties.maxAttempts()) {
                deadCounter.increment();
            } else {
                retryCounter.increment();
            }
            try {
                repository.registrarFallo(
                        reservada.id(),
                        instanceId(),
                        "FALLO_REINTENTABLE",
                        ex.getMessage(),
                        true,
                        properties.maxAttempts(),
                        properties.backoffBase(),
                        properties.backoffMax()
                );
            } catch (final Exception regEx) {
                LOGGER.warn("Fallo al registrar fallo reintentable en DB para id={}: {}", reservada.id(), regEx.getMessage());
                lostLeaseCounter.increment();
            }
        }
    }

    void verificarDepuracionPeriodica() {
        final Instant ahora = clock.instant();
        if (ahora.isAfter(ultimaDepuracion.plus(properties.retentionInterval()))) {
            ejecutarDepuracion();
        }
    }

    public void ejecutarDepuracion() {
        final boolean establishedCorrelation = CorrelationIdContext.get() == null;
        if (establishedCorrelation) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            final int eliminadas = repository.depurar(properties.retentionDays(), properties.batchSize() * 10);
            if (eliminadas > 0) {
                purgedCounter.increment(eliminadas);
                LOGGER.info("Depuradas {} notificaciones terminales del outbox.", eliminadas);
            }
            ultimaDepuracion = clock.instant();
        } catch (final Exception e) {
            LOGGER.warn("Fallo durante depuracion de outbox: {}", e.getMessage());
        } finally {
            if (establishedCorrelation) {
                CorrelationIdContext.clear();
            }
        }
    }

    private List<DestinatarioRef> deserializarDestinatarios(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DestinatarioRef>>() {});
        } catch (final Exception _) {
            return List.of();
        }
    }

    private Map<String, String> deserializarDatos(final String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (final Exception _) {
            return Map.of();
        }
    }
}
