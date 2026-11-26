package co.edu.uco.asistenciasuco.infrastructure.config.adapters.cache;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.RedisLimitadorIntentosAdapter;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.DisyuntorRedis;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.SinCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.decorador.ConsultasCacheadasPostProcessor;
import co.edu.uco.asistenciasuco.infrastructure.cache.decorador.InvalidacionCachePorEventos;
import co.edu.uco.asistenciasuco.infrastructure.cache.local.CaffeineCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.redis.RedisCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.CacheAdapterProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * REDIS-001: Composition Root de la cache de consultas. {@code app.adapters.cache.provider} elige la tecnologia:
 * {@code local} (Caffeine por replica, predeterminado), {@code redis} (compartida entre replicas) o {@code none}.
 * Los decoradores de {@link ConsultasCacheadasPostProcessor} son los mismos para los tres proveedores.
 */
@Configuration(proxyBeanMethods = false)
public class CacheAdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheAdapterConfiguration.class);

    @Bean
    public static ConsultasCacheadasPostProcessor consultasCacheadasPostProcessor(
            final ObjectProvider<CacheConsultas> cache,
            final ObjectProvider<InstitutionalScopePort> ambito,
            final ObjectProvider<DestinatariosReclamoPort> destinatariosReclamo) {
        return new ConsultasCacheadasPostProcessor(cache, ambito, destinatariosReclamo);
    }

    /** Code review 2026-11-26: un solo disyuntor por replica para la cache y el limitador sobre Redis. */
    @Bean
    public DisyuntorRedis disyuntorRedis() {
        return new DisyuntorRedis(Clock.systemUTC());
    }

    @Bean
    public CacheConsultas cacheConsultas(final CacheAdapterProperties properties, final MeterRegistry meterRegistry,
                                         final ObjectProvider<StringRedisTemplate> redis,
                                         final DisyuntorRedis disyuntorRedis,
                                         final Environment environment) {
        final CacheConsultas cache = switch (properties.provider()) {
            case NONE -> new SinCacheConsultas();
            case LOCAL -> new CaffeineCacheConsultas(properties.ttlPorRegion(), properties.maximoPorRegion(),
                    new MetricasCache(meterRegistry, "local"));
            case REDIS -> new RedisCacheConsultas(redis.getObject(), JsonMapper.builder().build(), properties.prefijo(),
                    properties.ttlPorRegion(), new MetricasCache(meterRegistry, "redis"), Clock.systemUTC(), disyuntorRedis);
        };
        LOGGER.info("Cache de consultas: proveedor={} ttl={}", cache.proveedor(), properties.ttl());
        if (properties.provider() == CacheAdapterProperties.Provider.LOCAL
                && !"redis-sse".equalsIgnoreCase(environment.getProperty("app.adapters.realtime.provider", "local-sse"))) {
            // Code review 2026-11-26: con cache local y realtime local-sse, la invalidacion solo llega a la replica donde
            // ocurrio el cambio. Con una replica no importa; con varias, las demas sirven datos viejos hasta el TTL.
            LOGGER.warn("Cache de consultas local con realtime local-sse: valido SOLO con una replica. Con varias replicas "
                    + "use app.adapters.cache.provider=redis (o none), o app.adapters.realtime.provider=redis-sse.");
        }
        return cache;
    }

    /**
     * TD-083: borra la cache del portal de un grupo cuando llega un evento que cambia sus asistencias, sesiones o padron.
     * Se engancha al adaptador SSE local, que recibe tanto los eventos de esta replica como los relevados por Redis.
     */
    @Bean
    public InvalidacionCachePorEventos invalidacionCachePorEventos(final ObjectProvider<CacheConsultas> cache,
                                                                   final ObjectProvider<ReactorRealtimeAdapter> realtime) {
        final InvalidacionCachePorEventos invalidacion = new InvalidacionCachePorEventos(cache::getObject);
        realtime.ifUnique(adaptador -> adaptador.alEntregar(invalidacion));
        return invalidacion;
    }

    /**
     * Con Redis, el limite por minuto del registro por QR (por estudiante y por token o PIN) se cuenta una sola vez para
     * todas las replicas. Si Redis no responde se usa el limitador en memoria de la replica.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.adapters.cache", name = "provider", havingValue = "redis")
    public LimitadorIntentosPort redisLimitadorIntentosPort(final StringRedisTemplate redis,
                                                           final CacheAdapterProperties properties,
                                                           @Qualifier("limitadorIntentosPort") final LimitadorIntentosPort limitadorIntentosPort,
                                                           final DisyuntorRedis disyuntorRedis) {
        return new RedisLimitadorIntentosAdapter(redis, properties.prefijo(), limitadorIntentosPort, Clock.systemUTC(),
                disyuntorRedis);
    }
}
