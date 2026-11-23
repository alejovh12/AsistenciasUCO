package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.catalogoparametros;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.AvisoCambioParametro;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.ResumenSincronizacion;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.RedeliveryBackoff;
import org.apache.pulsar.client.impl.MultiplierRedeliveryBackoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * INT-004: adaptador primario de los eventos del Catalogo de Parametros por Pulsar (schemaVersion 3).
 *
 * <p>Se suscribe (durable, {@code Shared}) al topic de la identidad de servicio del backend
 * ({@code <prefijo>-<clientId>}); el Catalogo solo publica alli los cambios de la aplicacion y el ambiente asociados a esa
 * identidad. Cada evento se entrega al puerto de entrada con un {@code correlationId} propio (logs y SP). Un evento procesado se
 * confirma (ack despues de procesar); si fallo (Catalogo o BD caidos) se devuelve al broker con espera creciente
 * ({@code negativeAcknowledge} con backoff multiplicador) para reintento. Un evento ilegible, de otra fuente o sin
 * {@code eventType} se confirma y se descarta (reintentarlo no lo arregla).</p>
 *
 * <p>La suscripcion es asincrona: si Pulsar no responde, el backend arranca igual y sigue leyendo parametros con su cache. Al
 * arrancar se reconcilia el catalogo completo en segundo plano con una sola lectura M2M (no es sondeo): cubre eventos perdidos o
 * emitidos con el backend apagado.</p>
 *
 * <p>Metricas ({@code /actuator/prometheus}): {@code catalogo_parametros_eventos_total{resultado}} con resultado en
 * {@code recibido, procesado, descartado, fallido}; {@code catalogo_parametros_valores_total{resultado}} con lo hecho en la BD;
 * {@code catalogo_parametros_ultima_sincronizacion_segundos} (epoch del ultimo evento o reconciliacion exitosos).</p>
 */
public class CatalogoParametrosEventListener implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoParametrosEventListener.class);

    static final String FUENTE_ESPERADA = "catalogo-parametros";
    static final int SCHEMA_VERSION = 3;
    static final String METRICA_EVENTOS = "catalogo_parametros_eventos_total";
    static final String METRICA_VALORES = "catalogo_parametros_valores_total";
    static final String METRICA_ULTIMA = "catalogo_parametros_ultima_sincronizacion_segundos";

    /** Espera inicial entre reintentos de suscripcion: {@value #ESPERA_INICIAL_SEGUNDOS} s, se duplica hasta 5 min. */
    static final long ESPERA_INICIAL_SEGUNDOS = 15;
    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(5);

    private final PulsarClient pulsarClient;
    private final SincronizarParametrosCatalogoInputPort sincronizacion;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String subscriptionName;
    private final boolean reconcileOnStartup;
    private final Duration esperaInicial;
    private final MeterRegistry metricas;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean suscrito = new AtomicBoolean(false);
    private final AtomicLong ultimaSincronizacion = new AtomicLong(0);

    /** Suscripcion vigente; start y stop pueden correr en hilos distintos del ciclo de vida. */
    private final AtomicReference<CompletableFuture<Consumer<String>>> consumer = new AtomicReference<>();

    public CatalogoParametrosEventListener(
            final PulsarClient pulsarClient,
            final SincronizarParametrosCatalogoInputPort sincronizacion,
            final ObjectMapper objectMapper,
            final String topic,
            final String subscriptionName,
            final boolean reconcileOnStartup,
            final MeterRegistry metricas
    ) {
        this(pulsarClient, sincronizacion, objectMapper, topic, subscriptionName, reconcileOnStartup, metricas,
                Duration.ofSeconds(ESPERA_INICIAL_SEGUNDOS));
    }

    CatalogoParametrosEventListener(
            final PulsarClient pulsarClient,
            final SincronizarParametrosCatalogoInputPort sincronizacion,
            final ObjectMapper objectMapper,
            final String topic,
            final String subscriptionName,
            final boolean reconcileOnStartup,
            final MeterRegistry metricas,
            final Duration esperaInicial
    ) {
        this.pulsarClient = Objects.requireNonNull(pulsarClient, "PulsarClient es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "El puerto de sincronizacion es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
        this.topic = Objects.requireNonNull(topic, "El topic es obligatorio.");
        this.subscriptionName = Objects.requireNonNull(subscriptionName, "La suscripcion es obligatoria.");
        this.reconcileOnStartup = reconcileOnStartup;
        this.metricas = Objects.requireNonNull(metricas, "MeterRegistry es obligatorio.");
        this.esperaInicial = Objects.requireNonNull(esperaInicial, "La espera entre reintentos es obligatoria.");
        if (esperaInicial.isZero() || esperaInicial.isNegative()) {
            throw new IllegalArgumentException("La espera entre reintentos de suscripcion debe ser positiva.");
        }
        this.metricas.gauge(METRICA_ULTIMA, ultimaSincronizacion, AtomicLong::get);
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        suscribir(1);
        if (reconcileOnStartup) {
            Thread.ofVirtual().name("catalogo-parametros-reconciliacion").start(this::reconciliar);
        }
    }

    /**
     * Pide la suscripcion; si falla (Pulsar aun cargando sus topics, broker caido) la reintenta con espera creciente mientras el
     * listener siga activo. Sin suscripcion el Backend sigue leyendo el Catalogo con su cache.
     */
    private void suscribir(final int intento) {
        if (!running.get()) {
            return;
        }
        final RedeliveryBackoff backoff = MultiplierRedeliveryBackoff.builder()
                .minDelayMs(1_000).maxDelayMs(60_000).multiplier(2.0).build();
        final CompletableFuture<Consumer<String>> suscripcion = pulsarClient.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.Shared)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .negativeAckRedeliveryBackoff(backoff)
                .messageListener(this::recibir)
                .subscribeAsync();
        consumer.set(suscripcion);
        suscripcion.whenComplete((suscrita, error) -> {
            if (error != null) {
                suscrito.set(false);
                if (!running.get()) {
                    return;
                }
                final Duration espera = esperaAntesDelIntento(intento);
                LOGGER.warn("No fue posible suscribirse a los eventos del Catalogo de Parametros; se reintenta en {} s y mientras tanto se lee con cache. topic={}, intento={}, causa={}",
                        espera.toSeconds(), topic, intento, error.getClass().getSimpleName());
                CompletableFuture.delayedExecutor(espera.toMillis(), TimeUnit.MILLISECONDS).execute(() -> suscribir(intento + 1));
            } else if (!running.get()) {
                suscrita.closeAsync().exceptionally(cierre -> null);
            } else {
                suscrito.set(true);
                LOGGER.info("Suscrito a los eventos del Catalogo de Parametros. topic={}, suscripcion={}, intento={}", topic, subscriptionName, intento);
            }
        });
    }

    /** Espera antes de reintentar despues del intento {@code intento} (1, 2, 3...): se duplica hasta un tope de 5 min. */
    Duration esperaAntesDelIntento(final int intento) {
        final int exponente = Math.max(0, Math.min(intento - 1, 20));
        final Duration espera = esperaInicial.multipliedBy(1L << exponente);
        return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        suscrito.set(false);
        final CompletableFuture<Consumer<String>> actual = consumer.getAndSet(null);
        if (actual != null) {
            actual.thenCompose(Consumer::closeAsync).exceptionally(error -> null);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** {@code true} mientras la suscripcion al topic esta activa (lo usa el indicador de salud). */
    public boolean estaSuscrito() {
        return suscrito.get();
    }

    /** Epoch (segundos) del ultimo evento o reconciliacion exitosos; 0 si aun no hubo. */
    public long ultimaSincronizacionEpochSegundos() {
        return ultimaSincronizacion.get();
    }

    void recibir(final Consumer<String> origen, final Message<String> mensaje) {
        if (procesar(mensaje.getValue())) {
            origen.acknowledgeAsync(mensaje);
        } else {
            origen.negativeAcknowledge(mensaje);
        }
    }

    /**
     * Procesa el JSON de un evento.
     *
     * @return {@code true} si el evento se debe confirmar (procesado, ajeno o ilegible); {@code false} si se debe reintentar.
     */
    boolean procesar(final String payload) {
        contar("recibido");
        final AvisoCambioParametro aviso;
        try {
            aviso = leer(payload);
        } catch (final JacksonException | IllegalArgumentException e) {
            contar("descartado");
            LOGGER.warn("Evento del Catalogo de Parametros ilegible; se descarta. causa={}", e.getClass().getSimpleName());
            return true;
        }
        if (aviso == null) {
            contar("descartado");
            return true;
        }
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            final ResumenSincronizacion resumen = sincronizacion.procesarAviso(aviso);
            registrarValores(resumen);
            ultimaSincronizacion.set(Instant.now().getEpochSecond());
            contar("procesado");
            LOGGER.info("Evento del Catalogo de Parametros procesado. tipo={}, nombre={}, idEvento={}, resumen={}",
                    aviso.tipo(), aviso.nombreRecurso(), aviso.idAviso(), resumen);
            return true;
        } catch (final RuntimeException e) {
            contar("fallido");
            LOGGER.warn("Evento del Catalogo de Parametros no procesado; se reintentara. tipo={}, nombre={}, idEvento={}, causa={}",
                    aviso.tipo(), aviso.nombreRecurso(), aviso.idAviso(), e.getClass().getSimpleName());
            return false;
        } finally {
            CorrelationIdContext.clear();
        }
    }

    void reconciliar() {
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            registrarValores(sincronizacion.reconciliarTodo());
            ultimaSincronizacion.set(Instant.now().getEpochSecond());
        } catch (final RuntimeException e) {
            LOGGER.warn("La reconciliacion inicial con el Catalogo de Parametros fallo; se sigue con cache y BD. causa={}",
                    e.getClass().getSimpleName());
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private void contar(final String resultado) {
        metricas.counter(METRICA_EVENTOS, "resultado", resultado).increment();
    }

    private void registrarValores(final ResumenSincronizacion resumen) {
        sumar("aplicado", resumen.aplicados());
        sumar("sin_cambios", resumen.sinCambios());
        sumar("desconocido", resumen.desconocidos());
        sumar("obsoleto", resumen.obsoletos());
        sumar("rechazado", resumen.rechazados());
        sumar("fallido", resumen.fallidos());
    }

    private void sumar(final String resultado, final int cantidad) {
        if (cantidad > 0) {
            metricas.counter(METRICA_VALORES, "resultado", resultado).increment(cantidad);
        }
    }

    /**
     * @return el evento, o {@code null} si no es de este Catalogo o no trae {@code eventType} (se descarta).
     */
    private AvisoCambioParametro leer(final String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Evento vacio.");
        }
        final JsonNode nodo = objectMapper.readTree(payload);
        if (!nodo.isObject()) {
            throw new IllegalArgumentException("El evento no es un objeto JSON.");
        }
        final String fuente = nodo.path("source").asString(null);
        final String tipo = nodo.path("eventType").asString(null);
        if (tipo == null || tipo.isBlank() || (fuente != null && !FUENTE_ESPERADA.equalsIgnoreCase(fuente.trim()))) {
            return null;
        }
        final int version = nodo.path("schemaVersion").asInt(SCHEMA_VERSION);
        if (version != SCHEMA_VERSION) {
            LOGGER.warn("Evento del Catalogo de Parametros con schemaVersion {} (se esperaba {}); se procesa igual porque el valor se relee por M2M.",
                    version, SCHEMA_VERSION);
        }
        return new AvisoCambioParametro(
                nodo.path("eventId").asString(null),
                tipo,
                nodo.path("resourceName").asString(null),
                nodo.path("applicationId").asString(null),
                nodo.path("environmentId").asString(null),
                instante(nodo.path("occurredAt").asString(null)));
    }

    private static Instant instante(final String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(texto.trim());
        } catch (final DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(texto.trim()).toInstant();
            } catch (final DateTimeParseException ignorado) {
                return null;
            }
        }
    }
}
