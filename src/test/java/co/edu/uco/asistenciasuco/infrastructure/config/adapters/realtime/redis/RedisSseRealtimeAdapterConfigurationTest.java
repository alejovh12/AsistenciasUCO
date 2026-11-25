package co.edu.uco.asistenciasuco.infrastructure.config.adapters.realtime.redis;

import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeRelayAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSseRealtimeAdapterConfigurationTest {

    private final RedisSseRealtimeAdapterConfiguration config = new RedisSseRealtimeAdapterConfiguration();

    @Test
    void arma_el_relevo_la_suscripcion_y_el_gateway() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final ReactorRealtimeAdapter local = config.reactorRealtimeAdapter(registry);
        final RedisRealtimeCodec codec = config.redisRealtimeCodec();
        final StringRedisTemplate redis = mock(StringRedisTemplate.class);

        final RealtimePublisherPort relevo = config.redisRealtimeRelayAdapter(redis, "canal", codec, local, registry);
        final RedisMessageListenerContainer contenedor = config.redisRealtimeListenerContainer(
                mock(RedisConnectionFactory.class), "canal", codec, local);
        final RealtimeStreamGateway gateway = config.realtimeStreamGateway(local, mock(ResolverAudienciaRealtimeInputPort.class), relevo);

        assertInstanceOf(RedisRealtimeRelayAdapter.class, relevo);
        assertNotNull(contenedor);
        gateway.emit("TOMA_ABIERTA", Map.of("grupo", "g-1"));
        verify(redis).convertAndSend(org.mockito.ArgumentMatchers.eq("canal"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void el_gateway_local_sigue_emitiendo_por_el_sink_local() {
        final ReactorRealtimeAdapter local = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        final RealtimePublisherPort publicador = mock(RealtimePublisherPort.class);
        final RealtimeStreamGateway gateway = co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse
                .LocalSseRealtimeStreamGateway.conRelevo(local, mock(ResolverAudienciaRealtimeInputPort.class), publicador);

        gateway.emit("TOMA_ABIERTA", Map.of("grupo", "g-1"));

        verify(publicador).publish(org.mockito.ArgumentMatchers.any(RealtimeEvent.class));
    }
}
