package co.edu.uco.asistenciasuco.infrastructure.config.adapters.cache;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.RedisLimitadorIntentosAdapter;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.SinCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.decorador.ConsultasCacheadasPostProcessor;
import co.edu.uco.asistenciasuco.infrastructure.cache.local.CaffeineCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.redis.RedisCacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.CacheAdapterProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheAdapterConfigurationTest {

    private final CacheAdapterConfiguration config = new CacheAdapterConfiguration();
    @SuppressWarnings("unchecked")
    private final ObjectProvider<StringRedisTemplate> redis = mock(ObjectProvider.class);

    private static CacheAdapterProperties propiedades(final CacheAdapterProperties.Provider provider) {
        return new CacheAdapterProperties(provider, null, null, null);
    }

    @Test
    void el_proveedor_elige_la_tecnologia() {
        when(redis.getObject()).thenReturn(mock(StringRedisTemplate.class));

        assertInstanceOf(CaffeineCacheConsultas.class,
                config.cacheConsultas(propiedades(CacheAdapterProperties.Provider.LOCAL), new SimpleMeterRegistry(), redis));
        assertInstanceOf(RedisCacheConsultas.class,
                config.cacheConsultas(propiedades(CacheAdapterProperties.Provider.REDIS), new SimpleMeterRegistry(), redis));
        assertInstanceOf(SinCacheConsultas.class,
                config.cacheConsultas(propiedades(CacheAdapterProperties.Provider.NONE), new SimpleMeterRegistry(), redis));
    }

    @Test
    void el_post_processor_y_el_limitador_redis_se_registran() {
        @SuppressWarnings("unchecked")
        final ObjectProvider<co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas> cache = mock(ObjectProvider.class);
        assertNotNull(CacheAdapterConfiguration.consultasCacheadasPostProcessor(cache));
        assertInstanceOf(ConsultasCacheadasPostProcessor.class, CacheAdapterConfiguration.consultasCacheadasPostProcessor(cache));
        assertInstanceOf(RedisLimitadorIntentosAdapter.class, config.redisLimitadorIntentosPort(mock(StringRedisTemplate.class),
                propiedades(CacheAdapterProperties.Provider.REDIS), mock(LimitadorIntentosPort.class)));
    }

    @Test
    void las_propiedades_tienen_valores_por_defecto() {
        final CacheAdapterProperties vacias = new CacheAdapterProperties(null, null, 0L, " ");

        assertEquals(CacheAdapterProperties.Provider.LOCAL, vacias.provider());
        assertEquals(10_000L, vacias.maximoPorRegion());
        assertEquals("asistencias-uco", vacias.prefijo());
        assertEquals(Duration.ofMinutes(30), vacias.ttlPorRegion().get(RegionCache.CATALOGOS));
        assertEquals(Duration.ofMinutes(5), vacias.ttlPorRegion().get(RegionCache.INDICADORES));
        assertEquals(Duration.ofSeconds(60), vacias.ttlPorRegion().get(RegionCache.PORTAL));
        assertEquals(Duration.ofSeconds(20),
                new CacheAdapterProperties.Ttl(null, null, Duration.ofSeconds(20)).portal());
    }

    @Test
    void un_ttl_fuera_de_rango_falla_el_arranque() {
        final Duration cero = Duration.ZERO;
        final Duration negativo = Duration.ofSeconds(-1);
        final Duration enorme = Duration.ofHours(25);
        assertThrows(IllegalArgumentException.class, () -> new CacheAdapterProperties.Ttl(cero, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CacheAdapterProperties.Ttl(null, negativo, null));
        assertThrows(IllegalArgumentException.class, () -> new CacheAdapterProperties.Ttl(null, null, enorme));
    }
}
