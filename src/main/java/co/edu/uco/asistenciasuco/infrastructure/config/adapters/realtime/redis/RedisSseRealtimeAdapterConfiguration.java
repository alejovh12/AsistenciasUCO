package co.edu.uco.asistenciasuco.infrastructure.config.adapters.realtime.redis;

import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeRelayAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeSuscriptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * REDIS-001 (TD-003): Composition Root del provider realtime {@code redis-sse}. Cada replica conserva su sink Reactor y su
 * canal SSE (mismo contrato HTTP, mismo filtro por grupo y audiencia); los casos de uso publican por el relevo Redis y
 * cada replica entrega lo que recibe del canal a sus propios clientes.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.realtime", name = "provider", havingValue = "redis-sse")
public class RedisSseRealtimeAdapterConfiguration {

    @Bean
    public ReactorRealtimeAdapter reactorRealtimeAdapter(final MeterRegistry meterRegistry) {
        return new ReactorRealtimeAdapter(meterRegistry);
    }

    @Bean
    public RedisRealtimeCodec redisRealtimeCodec() {
        return new RedisRealtimeCodec(JsonMapper.builder().build());
    }

    @Bean
    @Primary
    public RealtimePublisherPort redisRealtimeRelayAdapter(
            final StringRedisTemplate redis,
            @Value("${app.adapters.realtime.redis.canal:asistencias-uco:realtime:v1}") final String canal,
            final RedisRealtimeCodec codec,
            final ReactorRealtimeAdapter reactorRealtimeAdapter,
            final MeterRegistry meterRegistry
    ) {
        return new RedisRealtimeRelayAdapter(redis, canal, codec, reactorRealtimeAdapter, meterRegistry);
    }

    @Bean
    public RedisMessageListenerContainer redisRealtimeListenerContainer(
            final RedisConnectionFactory connectionFactory,
            @Value("${app.adapters.realtime.redis.canal:asistencias-uco:realtime:v1}") final String canal,
            final RedisRealtimeCodec codec,
            final ReactorRealtimeAdapter reactorRealtimeAdapter
    ) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new RedisRealtimeSuscriptor(codec, reactorRealtimeAdapter), new ChannelTopic(canal));
        return container;
    }

    @Bean
    public RealtimeStreamGateway realtimeStreamGateway(
            final ReactorRealtimeAdapter reactorRealtimeAdapter,
            final ResolverAudienciaRealtimeInputPort resolverAudiencia,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return LocalSseRealtimeStreamGateway.conRelevo(reactorRealtimeAdapter, resolverAudiencia, realtimePublisherPort);
    }
}
