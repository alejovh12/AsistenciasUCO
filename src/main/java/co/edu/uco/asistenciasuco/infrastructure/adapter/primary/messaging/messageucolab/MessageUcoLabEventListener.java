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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * INT-003: adaptador primario de los avisos de MessageUcoLab 0.3.0 por Pulsar.
 *
 * <p>Se suscribe al topic de la identidad de servicio del backend ({@code <prefijo>-<clientId>}); MessageUcoLab solo
 * publica alli los cambios del ambiente asociado a esa identidad. Cada aviso se entrega al puerto de entrada con un
 * {@code correlationId} propio (logs y SP). Un aviso procesado se confirma; si fallo (BD o MessageUcoLab caidos) se
 * devuelve al broker para reintento. Un aviso ilegible se confirma y se descarta (reintentarlo no lo arregla).</p>
 *
 * <p>La suscripcion es asincrona: si Pulsar no responde, el backend arranca igual y sigue leyendo mensajes con su cache.
 * Al arrancar se reconcilia todo el catalogo en segundo plano (cubre avisos perdidos o emitidos con el backend apagado).</p>
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

    private volatile CompletableFuture<Consumer<String>> consumer;

    public MessageUcoLabEventListener(
            final PulsarClient pulsarClient,
            final SincronizarMensajesCatalogoInputPort sincronizacion,
            final ObjectMapper objectMapper,
            final String topic,
            final String subscriptionName,
            final boolean reconcileOnStartup
    ) {
        this.pulsarClient = Objects.requireNonNull(pulsarClient, "PulsarClient es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "El puerto de sincronizacion es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        this.topic = Objects.requireNonNull(topic, "El topic es obligatorio.");
        this.subscriptionName = Objects.requireNonNull(subscriptionName, "La suscripcion es obligatoria.");
        this.reconcileOnStartup = reconcileOnStartup;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        consumer = pulsarClient.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.Failover)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .messageListener(this::recibir)
                .subscribeAsync();
        consumer.whenComplete((suscrito, error) -> {
            if (error != null) {
                LOGGER.warn("No fue posible suscribirse a los avisos de MessageUcoLab; se sigue leyendo con cache. topic={}, causa={}",
                        topic, error.getClass().getSimpleName());
            } else {
                LOGGER.info("Suscrito a los avisos de MessageUcoLab. topic={}, suscripcion={}", topic, subscriptionName);
            }
        });
        if (reconcileOnStartup) {
            Thread.ofVirtual().name("messageucolab-reconciliacion").start(this::reconciliar);
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        final CompletableFuture<Consumer<String>> actual = consumer;
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
