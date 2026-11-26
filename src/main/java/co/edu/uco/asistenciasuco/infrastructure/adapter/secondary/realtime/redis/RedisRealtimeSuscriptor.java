package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * REDIS-001: recibe los eventos del canal Redis del realtime y los entrega a los clientes SSE de esta replica (el filtro
 * por grupo y audiencia sigue en {@code LocalSseRealtimeStreamGateway}). Los mensajes que publico esta misma replica se
 * ignoran (ya se entregaron al publicarlos). Un mensaje ilegible se descarta sin cortar la suscripcion.
 */
public final class RedisRealtimeSuscriptor implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRealtimeSuscriptor.class);

    private final RedisRealtimeCodec codec;
    private final ReactorRealtimeAdapter local;

    public RedisRealtimeSuscriptor(final RedisRealtimeCodec codec, final ReactorRealtimeAdapter local) {
        this.codec = Objects.requireNonNull(codec, "RedisRealtimeCodec es obligatorio.");
        this.local = Objects.requireNonNull(local, "ReactorRealtimeAdapter es obligatorio.");
    }

    @Override
    public void onMessage(final Message message, final byte[] pattern) {
        try {
            final RealtimeEvent event = codec.leerAjeno(new String(message.getBody(), StandardCharsets.UTF_8));
            if (event != null) {
                local.entregarRelevado(event);
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("Mensaje realtime de Redis ilegible; se descarta. causa={}", exception.getClass().getSimpleName());
        }
    }
}
