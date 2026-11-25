package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * REDIS-001 (TD-003): publica los eventos realtime en un canal Redis Pub/Sub para que TODAS las replicas los entreguen a
 * sus clientes SSE. La replica de origen tambien los recibe por la suscripcion, asi que nunca se entregan dos veces.
 *
 * <p>Mismas garantias que el SSE local: best-effort, sin replay ni durabilidad (Pub/Sub no guarda mensajes; el cliente
 * recupera el estado por la API al reconectar). Si Redis no responde, el evento se entrega solo en esta replica
 * (degradacion al comportamiento previo) y se cuenta en {@code realtime.relay.fallback}. Nunca lanza excepciones al caso
 * de uso (contrato de {@link RealtimePublisherPort}).</p>
 */
public final class RedisRealtimeRelayAdapter implements RealtimePublisherPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRealtimeRelayAdapter.class);

    private final StringRedisTemplate redis;
    private final String canal;
    private final RedisRealtimeCodec codec;
    private final ReactorRealtimeAdapter local;
    private final Counter relevados;
    private final Counter respaldos;

    public RedisRealtimeRelayAdapter(final StringRedisTemplate redis, final String canal, final RedisRealtimeCodec codec,
                                     final ReactorRealtimeAdapter local, final MeterRegistry meterRegistry) {
        this.redis = Objects.requireNonNull(redis, "StringRedisTemplate es obligatorio.");
        this.canal = Objects.requireNonNull(canal, "El canal Redis del realtime es obligatorio.");
        this.codec = Objects.requireNonNull(codec, "RedisRealtimeCodec es obligatorio.");
        this.local = Objects.requireNonNull(local, "ReactorRealtimeAdapter es obligatorio.");
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.relevados = Counter.builder("realtime.relay.published")
                .description("Eventos realtime enviados al canal Redis para todas las replicas.")
                .register(meterRegistry);
        this.respaldos = Counter.builder("realtime.relay.fallback")
                .description("Eventos realtime entregados solo en la replica local porque Redis no respondio.")
                .register(meterRegistry);
    }

    @Override
    public void publish(final RealtimeEvent event) {
        if (event == null) {
            local.publish(null);
            return;
        }
        final RealtimeEvent preparado;
        try {
            preparado = local.prepararParaRelevo(event);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No se pudo preparar el evento realtime {} para el relevo; se descarta.", event.type());
            return;
        }
        try {
            redis.convertAndSend(canal, codec.escribir(preparado));
            relevados.increment();
        } catch (final RuntimeException exception) {
            respaldos.increment();
            LOGGER.warn("Relevo Redis del realtime no disponible; el evento {} se entrega solo en esta replica. causa={}",
                    preparado.type(), exception.getClass().getSimpleName());
            local.entregarRelevado(preparado);
        }
    }
}
