package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.messageucolab;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.AvisoCambioMensaje;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.ResumenSincronizacion;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * INT-003: adaptador primario de los avisos de MessageUcoLab 0.3.0 por Pulsar.
 *
 * <p>Se suscribe al topic de la identidad de servicio del backend ({@code <prefijo>-<clientId>}); MessageUcoLab solo
 * publica alli los cambios del ambiente asociado a esa identidad. Cada aviso se entrega al puerto de entrada con un
 * {@code correlationId} propio (logs y SP). Un aviso procesado se confirma; si fallo (BD o MessageUcoLab caidos) se
 * devuelve al broker para reintento. Un aviso ilegible se confirma y se descarta (reintentarlo no lo arregla).</p>
 *
 * <p>La suscripcion es asincrona: si Pulsar no responde, el backend arranca igual y sigue leyendo mensajes con su cache.
 * Al arrancar se reconcilia el catalogo completo en segundo plano (cubre avisos perdidos o emitidos con el backend apagado).</p>
 */
public class MessageUcoLabEventListener implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUcoLabEventListener.class);

    private final PulsarClient pulsarClient;
    private final SincronizarMensajesCatalogoInputPort sincronizacion;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String subscriptionName;
    private final boolean reconcileOnStartup;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Duration esperaInicial;

    /** Suscripcion vigente; start y stop pueden correr en hilos distintos del ciclo de vida. */
    private final AtomicReference<CompletableFuture<Consumer<String>>> consumer = new AtomicReference<>();

    /** Espera inicial entre reintentos de suscripcion: {@value #ESPERA_INICIAL_SEGUNDOS} s, se duplica hasta 5 min. */
    static final long ESPERA_INICIAL_SEGUNDOS = 15;
    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(5);

    public MessageUcoLabEventListener(
            final PulsarClient pulsarClient,
            final SincronizarMensajesCatalogoInputPort sincronizacion,
            final ObjectMapper objectMapper,
            final String topic,
            final String subscriptionName,
            final boolean reconcileOnStartup
    ) {
        this(pulsarClient, sincronizacion, objectMapper, topic, subscriptionName, reconcileOnStartup,
                Duration.ofSeconds(ESPERA_INICIAL_SEGUNDOS));
    }

    MessageUcoLabEventListener(
            final PulsarClient pulsarClient,
            final SincronizarMensajesCatalogoInputPort sincronizacion,
            final ObjectMapper objectMapper,
            final String topic,
            final String subscriptionName,
            final boolean reconcileOnStartup,
            final Duration esperaInicial
    ) {
        this.pulsarClient = Objects.requireNonNull(pulsarClient, "PulsarClient es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "El puerto de sincronizacion es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        this.topic = Objects.requireNonNull(topic, "El topic es obligatorio.");
        this.subscriptionName = Objects.requireNonNull(subscriptionName, "La suscripcion es obligatoria.");
        this.reconcileOnStartup = reconcileOnStartup;
        this.esperaInicial = Objects.requireNonNull(esperaInicial, "La espera entre reintentos es obligatoria.");
        if (esperaInicial.isZero() || esperaInicial.isNegative()) {
            throw new IllegalArgumentException("La espera entre reintentos de suscripcion debe ser positiva.");
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        suscribir(1);
        if (reconcileOnStartup) {
            Thread.ofVirtual().name("messageucolab-reconciliacion").start(this::reconciliar);
        }
    }

    /**
     * Pide la suscripcion; si falla (Pulsar aun cargando sus topics, broker caido) la reintenta con espera creciente
     * mientras el listener siga activo. Sin suscripcion el Backend sigue leyendo MessageUcoLab con su cache.
     */
    private void suscribir(final int intento) {
        if (!running.get()) {
            return;
        }
        final CompletableFuture<Consumer<String>> suscripcion = pulsarClient.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.Failover)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .messageListener(this::recibir)
                .subscribeAsync();
        consumer.set(suscripcion);
        suscripcion.whenComplete((suscrito, error) -> {
            if (error != null) {
                if (!running.get()) {
                    return;
                }
                final Duration espera = esperaAntesDelIntento(intento);
                LOGGER.warn("No fue posible suscribirse a los avisos de MessageUcoLab; se reintenta en {} s y mientras tanto se lee con cache. topic={}, intento={}, causa={}",
                        espera.toSeconds(), topic, intento, error.getClass().getSimpleName());
                CompletableFuture.delayedExecutor(espera.toMillis(), TimeUnit.MILLISECONDS).execute(() -> suscribir(intento + 1));
            } else if (!running.get()) {
                suscrito.closeAsync().exceptionally(cierre -> null);
            } else {
                LOGGER.info("Suscrito a los avisos de MessageUcoLab. topic={}, suscripcion={}, intento={}", topic, subscriptionName, intento);
            }
        });
    }

    /** Espera antes de reintentar despues del intento {@code intento} (1, 2, 3...): se duplica hasta un tope de 5 min. */
    Duration esperaAntesDelIntento(final int intento) {
        final int exponente = Math.clamp(intento - 1, 0, 20);
        final Duration espera = esperaInicial.multipliedBy(1L << exponente);
        return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        final CompletableFuture<Consumer<String>> actual = consumer.getAndSet(null);
        if (actual != null) {
            actual.thenCompose(Consumer::closeAsync).exceptionally(error -> null);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    void recibir(final Consumer<String> origen, final Message<String> mensaje) {
        if (procesar(mensaje.getValue())) {
            origen.acknowledgeAsync(mensaje);
        } else {
            origen.negativeAcknowledge(mensaje);
        }
    }

    /**
     * Procesa el JSON de un aviso.
     *
     * @return {@code true} si el aviso se debe confirmar (procesado o ilegible); {@code false} si se debe reintentar.
     */
    boolean procesar(final String payload) {
        final AvisoCambioMensaje aviso;
        try {
            aviso = leer(payload);
        } catch (final JacksonException | IllegalArgumentException e) {
            LOGGER.warn("Aviso de MessageUcoLab ilegible; se descarta. causa={}", e.getClass().getSimpleName());
            return true;
        }
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            final ResumenSincronizacion resumen = sincronizacion.procesarAviso(aviso);
            LOGGER.info("Aviso de MessageUcoLab procesado. tipo={}, codigo={}, idAviso={}, resumen={}",
                    aviso.tipo(), aviso.codigoExterno(), aviso.idAviso(), resumen);
            return true;
        } catch (final RuntimeException e) {
            LOGGER.warn("Aviso de MessageUcoLab no procesado; se reintentara. tipo={}, codigo={}, causa={}",
                    aviso.tipo(), aviso.codigoExterno(), e.getClass().getSimpleName());
            return false;
        } finally {
            CorrelationIdContext.clear();
        }
    }

    void reconciliar() {
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            sincronizacion.reconciliarTodo();
        } catch (final RuntimeException e) {
            LOGGER.warn("La reconciliacion inicial con MessageUcoLab fallo. causa={}", e.getClass().getSimpleName());
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private AvisoCambioMensaje leer(final String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Aviso vacio.");
        }
        final JsonNode nodo = objectMapper.readTree(payload);
        if (!nodo.isObject()) {
            throw new IllegalArgumentException("El aviso no es un objeto JSON.");
        }
        return new AvisoCambioMensaje(
                nodo.path("eventId").asString(null),
                nodo.path("eventType").asString(null),
                nodo.path("messageCode").asString(null));
    }
}
