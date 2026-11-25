package co.edu.uco.asistenciasuco.infrastructure.cache.integracion;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeRelayAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis.RedisRealtimeSuscriptor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.RedisLimitadorIntentosAdapter;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.redis.RedisCacheConsultas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REDIS-001: prueba contra un Redis real. Solo corre si existe {@code REDIS_TEST_HOST} (por ejemplo
 * {@code docker run -p 6379:6379 redis:7-alpine} y {@code REDIS_TEST_HOST=localhost}); en CI queda omitida y la cubren las
 * pruebas con dobles. Usa la base 15 y un prefijo aleatorio para no tocar datos de otros.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_HOST", matches = ".+")
class RedisRealIntegrationTest {

    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private final String prefijo = "prueba-" + UUID.randomUUID();
    private final List<RedisMessageListenerContainer> contenedores = new CopyOnWriteArrayList<>();

    @BeforeEach
    void conectar() {
        final RedisStandaloneConfiguration configuracion = new RedisStandaloneConfiguration(System.getenv("REDIS_TEST_HOST"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_TEST_PORT", "6379")));
        configuracion.setDatabase(15);
        factory = new LettuceConnectionFactory(configuracion);
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
    }

    @AfterEach
    void cerrar() throws Exception {
        for (final RedisMessageListenerContainer contenedor : contenedores) {
            contenedor.stop();
            contenedor.destroy();
        }
        final var claves = redis.keys(prefijo + "*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
        factory.destroy();
    }

    @Test
    void dos_replicas_comparten_la_cache_y_respetan_el_ttl() throws InterruptedException {
        final Map<RegionCache, Duration> ttl = new EnumMap<>(RegionCache.class);
        ttl.put(RegionCache.CATALOGOS, Duration.ofMinutes(1));
        ttl.put(RegionCache.INDICADORES, Duration.ofMinutes(1));
        ttl.put(RegionCache.PORTAL, Duration.ofSeconds(1));
        final RedisCacheConsultas replicaA = new RedisCacheConsultas(redis, JsonMapper.builder().build(), prefijo, ttl,
                new MetricasCache(new SimpleMeterRegistry(), "redis"), Clock.systemUTC());
        final RedisCacheConsultas replicaB = new RedisCacheConsultas(redis, JsonMapper.builder().build(), prefijo, ttl,
                new MetricasCache(new SimpleMeterRegistry(), "redis"), Clock.systemUTC());
        final TypeReference<List<String>> tipo = new TypeReference<>() { };
        final AtomicInteger cargas = new AtomicInteger();

        assertEquals(List.of("m1"), replicaA.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet())));
        assertEquals(List.of("m1"), replicaB.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet())));
        assertEquals(1, cargas.get(), "la replica B usa lo que cargo la A");

        Thread.sleep(1_300);
        assertEquals(List.of("m2"), replicaB.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet())));

        replicaA.invalidar(RegionCache.PORTAL, "u1:materias");
        assertEquals(List.of("m3"), replicaB.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet())));
    }

    @Test
    void una_etiqueta_invalidada_en_una_replica_borra_las_claves_para_todas() {
        final Map<RegionCache, Duration> ttl = new EnumMap<>(RegionCache.class);
        for (final RegionCache region : RegionCache.values()) {
            ttl.put(region, Duration.ofMinutes(1));
        }
        final RedisCacheConsultas replicaA = new RedisCacheConsultas(redis, JsonMapper.builder().build(), prefijo, ttl,
                new MetricasCache(new SimpleMeterRegistry(), "redis"), Clock.systemUTC());
        final RedisCacheConsultas replicaB = new RedisCacheConsultas(redis, JsonMapper.builder().build(), prefijo, ttl,
                new MetricasCache(new SimpleMeterRegistry(), "redis"), Clock.systemUTC());
        final TypeReference<List<String>> tipo = new TypeReference<>() { };
        final AtomicInteger cargas = new AtomicInteger();

        replicaA.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet()));
        replicaA.obtener(RegionCache.PORTAL, "u2:materias", tipo, () -> List.of("x" + cargas.incrementAndGet()));
        replicaA.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("u1:materias", "u1:resumen"));
        final Long vence = redis.getExpire(prefijo + ":cache:v1:portal:etiqueta:grupo:g1");
        assertTrue(vence != null && vence > 0 && vence <= 60, "la etiqueta vence con el TTL de la region");

        replicaB.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");

        assertEquals(List.of("m3"), replicaA.obtener(RegionCache.PORTAL, "u1:materias", tipo, () -> List.of("m" + cargas.incrementAndGet())));
        assertEquals(List.of("x2"), replicaA.obtener(RegionCache.PORTAL, "u2:materias", tipo, () -> List.of("x" + cargas.incrementAndGet())));
    }

    @Test
    void el_limite_por_minuto_se_cuenta_entre_replicas() {
        final LimitadorIntentosPort respaldo = (clave, limite) -> true;
        final RedisLimitadorIntentosAdapter replicaA = new RedisLimitadorIntentosAdapter(redis, prefijo, respaldo, Clock.systemUTC());
        final RedisLimitadorIntentosAdapter replicaB = new RedisLimitadorIntentosAdapter(redis, prefijo, respaldo, Clock.systemUTC());

        assertTrue(replicaA.permitir("asistencia-qr:estudiante:u1", 2));
        assertTrue(replicaB.permitir("asistencia-qr:estudiante:u1", 2));
        assertFalse(replicaA.permitir("asistencia-qr:estudiante:u1", 2));
    }

    @Test
    void un_evento_publicado_en_una_replica_llega_a_los_clientes_de_la_otra() throws InterruptedException {
        final String canal = prefijo + ":realtime:v1";
        final RedisRealtimeCodec codec = new RedisRealtimeCodec(JsonMapper.builder().build());
        final ReactorRealtimeAdapter localA = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        final ReactorRealtimeAdapter localB = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        suscribir(canal, codec, localA);
        suscribir(canal, codec, localB);
        final List<RealtimeEvent> recibidosA = new CopyOnWriteArrayList<>();
        final List<RealtimeEvent> recibidosB = new CopyOnWriteArrayList<>();
        localA.events().subscribe(recibidosA::add);
        localB.events().subscribe(recibidosB::add);
        Thread.sleep(500);
        final RedisRealtimeRelayAdapter relevoA = new RedisRealtimeRelayAdapter(redis, canal, codec, localA, new SimpleMeterRegistry());
        final RealtimeEvent evento = RealtimeEvent.of("TOMA_ABIERTA", Map.of("grupo", "g-1"));

        relevoA.publish(evento);

        final long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while ((recibidosA.isEmpty() || recibidosB.isEmpty()) && System.nanoTime() < limite) {
            Thread.sleep(20);
        }
        Thread.sleep(200);
        assertEquals(1, recibidosA.size(), "la replica de origen lo entrega una sola vez");
        assertEquals(1, recibidosB.size(), "la otra replica tambien lo entrega");
        assertEquals(evento.eventId(), recibidosB.getFirst().eventId());
    }

    private void suscribir(final String canal, final RedisRealtimeCodec codec, final ReactorRealtimeAdapter local) {
        final RedisMessageListenerContainer contenedor = new RedisMessageListenerContainer();
        contenedor.setConnectionFactory(factory);
        contenedor.addMessageListener(new RedisRealtimeSuscriptor(codec, local), new ChannelTopic(canal));
        contenedor.afterPropertiesSet();
        contenedor.start();
        contenedores.add(contenedor);
    }
}
