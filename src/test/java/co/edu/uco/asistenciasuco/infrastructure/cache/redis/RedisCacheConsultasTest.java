package co.edu.uco.asistenciasuco.infrastructure.cache.redis;

import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheConsultasTest {

    record Fila(UUID id, String nombre, BigDecimal horas, LocalDate fecha) {
    }

    private static final TypeReference<List<Fila>> TIPO = new TypeReference<>() { };
    private static final Fila FILA = new Fila(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Calculo",
            new BigDecimal("4.50"), LocalDate.of(2026, 11, 25));
    private static final String CLAVE = "asistencias-uco:cache:v1:portal:u1:materias";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valores = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final SetOperations<String, String> conjuntos = mock(SetOperations.class);
    private RedisCacheConsultas cache;

    @BeforeEach
    void preparar() {
        when(redis.opsForValue()).thenReturn(valores);
        when(redis.opsForSet()).thenReturn(conjuntos);
        cache = new RedisCacheConsultas(redis, mapper, "asistencias-uco", ttl(), new MetricasCache(registry, "redis"),
                Clock.systemUTC());
    }

    private static Map<RegionCache, Duration> ttl() {
        final Map<RegionCache, Duration> mapa = new EnumMap<>(RegionCache.class);
        mapa.put(RegionCache.CATALOGOS, Duration.ofMinutes(30));
        mapa.put(RegionCache.INDICADORES, Duration.ofMinutes(5));
        mapa.put(RegionCache.PORTAL, Duration.ofSeconds(60));
        return mapa;
    }

    private double contador(final String resultado) {
        return registry.get("asistencias.cache.consultas").tag("region", "portal").tag("resultado", resultado).counter().count();
    }

    @Test
    void un_acierto_reconstruye_el_valor_sin_ir_a_la_fuente() {
        when(valores.get(CLAVE)).thenReturn(mapper.writeValueAsString(List.of(FILA)));

        final List<Fila> resultado = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
            throw new AssertionError("no debe ir a la BD");
        });

        assertEquals(List.of(FILA), resultado);
        assertEquals(1.0, contador("acierto"));
        assertEquals("redis", cache.proveedor());
    }

    @Test
    void un_fallo_carga_de_la_fuente_y_guarda_con_el_ttl_de_la_region() {
        final List<Fila> resultado = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));

        assertEquals(List.of(FILA), resultado);
        verify(valores).set(CLAVE, mapper.writeValueAsString(List.of(FILA)), Duration.ofSeconds(60));
        assertEquals(1.0, contador("fallo"));
    }

    @Test
    void un_resultado_nulo_no_se_guarda() {
        assertNull(cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> null));

        verify(valores, never()).set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void con_redis_caido_la_consulta_se_atiende_desde_la_fuente() {
        when(valores.get(CLAVE)).thenThrow(new RedisConnectionFailureException("caido"));

        final List<Fila> primera = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));
        final List<Fila> segunda = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));

        assertEquals(List.of(FILA), primera);
        assertEquals(List.of(FILA), segunda);
        assertEquals(2.0, contador("error"));
    }

    @Test
    void si_no_se_puede_guardar_igual_devuelve_el_valor() {
        doThrow(new RedisConnectionFailureException("caido")).when(valores)
                .set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));

        assertEquals(List.of(FILA), cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA)));
        assertEquals(1.0, contador("error"));
    }

    @Test
    void un_valor_ilegible_se_descarta_y_se_recarga() {
        when(valores.get(CLAVE)).thenReturn("{no es json");

        assertEquals(List.of(FILA), cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA)));
        verify(redis).delete(CLAVE);
    }

    @Test
    void invalidar_borra_la_clave_y_tolera_redis_caido() {
        cache.invalidar(RegionCache.PORTAL, "u1:materias");
        verify(redis).delete(CLAVE);

        when(redis.delete(CLAVE)).thenThrow(new RedisConnectionFailureException("caido"));
        cache.invalidar(RegionCache.PORTAL, "u1:materias");
        assertEquals(1.0, contador("error"));
    }

    @Test
    void una_falla_de_la_fuente_se_propaga() {
        assertThrows(IllegalStateException.class, () -> cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
            throw new IllegalStateException("bd");
        }));
    }

    @Test
    void muchas_peticiones_simultaneas_de_la_misma_clave_cargan_una_sola_vez() throws Exception {
        final AtomicInteger cargas = new AtomicInteger();
        final CountDownLatch dentroDeLaCarga = new CountDownLatch(1);
        final CountDownLatch liberar = new CountDownLatch(1);
        final ExecutorService hilos = Executors.newFixedThreadPool(8);
        try {
            final Future<List<Fila>> primera = hilos.submit(() -> cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
                cargas.incrementAndGet();
                dentroDeLaCarga.countDown();
                esperar(liberar);
                return List.of(FILA);
            }));
            assertTrue(dentroDeLaCarga.await(2, TimeUnit.SECONDS));
            final List<Future<List<Fila>>> demas = new java.util.ArrayList<>();
            for (int i = 0; i < 7; i++) {
                demas.add(hilos.submit(() -> cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
                    cargas.incrementAndGet();
                    return List.of(FILA);
                })));
            }
            Thread.sleep(100);
            liberar.countDown();

            assertEquals(List.of(FILA), primera.get(2, TimeUnit.SECONDS));
            for (final Future<List<Fila>> otra : demas) {
                assertEquals(List.of(FILA), otra.get(2, TimeUnit.SECONDS));
            }
            assertEquals(1, cargas.get());
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void quienes_esperan_una_carga_fallida_reciben_la_misma_falla() throws Exception {
        final CountDownLatch dentroDeLaCarga = new CountDownLatch(1);
        final CountDownLatch liberar = new CountDownLatch(1);
        final ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            final Future<List<Fila>> primera = hilos.submit(() -> cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
                dentroDeLaCarga.countDown();
                esperar(liberar);
                throw new IllegalStateException("bd");
            }));
            assertTrue(dentroDeLaCarga.await(2, TimeUnit.SECONDS));
            final Future<List<Fila>> segunda = hilos.submit(() -> cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO,
                    () -> List.of(FILA)));
            Thread.sleep(100);
            liberar.countDown();

            final Exception primeraFalla = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> primera.get(2, TimeUnit.SECONDS));
            assertTrue(primeraFalla.getCause() instanceof IllegalStateException);
            final Exception segundaFalla = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> segunda.get(2, TimeUnit.SECONDS));
            assertTrue(segundaFalla.getCause() instanceof IllegalStateException);
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void falta_el_ttl_de_una_region_falla_al_construir() {
        final Map<RegionCache, Duration> incompleto = new EnumMap<>(RegionCache.class);
        final MetricasCache metricas = new MetricasCache(new SimpleMeterRegistry(), "redis");
        final Clock clock = Clock.systemUTC();
        assertThrows(NullPointerException.class,
                () -> new RedisCacheConsultas(redis, mapper, "p", incompleto, metricas, clock));
    }

    private static void esperar(final CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static final String ETIQUETA = "asistencias-uco:cache:v1:portal:etiqueta:grupo:g1";

    @Test
    void etiquetar_agrega_las_claves_redis_al_set_con_el_ttl_de_la_region() {
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("u1:materias", "u1:resumen"));
        cache.etiquetar(RegionCache.PORTAL, java.util.Arrays.asList((String) null), List.of("u1:materias"));
        cache.etiquetar(RegionCache.PORTAL, null, List.of("u1:materias"));
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), null);
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of());

        verify(conjuntos).add(ETIQUETA, CLAVE, "asistencias-uco:cache:v1:portal:u1:resumen");
        verify(redis).expire(ETIQUETA, Duration.ofSeconds(60));
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidar_una_etiqueta_borra_sus_claves_y_el_set() {
        when(conjuntos.members(ETIQUETA)).thenReturn(Set.of(CLAVE));

        cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");
        cache.invalidarEtiqueta(RegionCache.PORTAL, null);

        final org.mockito.ArgumentCaptor<java.util.Collection<String>> borradas = org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
        verify(redis).delete(borradas.capture());
        assertEquals(List.of(CLAVE, ETIQUETA), List.copyOf(borradas.getValue()));
    }

    @Test
    void una_etiqueta_sin_miembros_borra_solo_el_set() {
        when(conjuntos.members(ETIQUETA)).thenReturn(null);

        cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");

        verify(redis).delete(List.of(ETIQUETA));
    }

    @Test
    void etiquetas_con_redis_caido_no_fallan_y_cuentan_error() {
        doThrow(new RedisConnectionFailureException("caido")).when(conjuntos).add(anyString(), any(String[].class));
        doThrow(new RedisConnectionFailureException("caido")).when(conjuntos).members(anyString());

        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("u1:materias"));
        cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");

        assertEquals(2.0, contador("error"));
    }
    // --- Code review 2026-11-26: disyuntor, invalidaciones pendientes y carrera carga/invalidacion ---

    private static final class RelojMovil extends Clock {
        private Instant ahora = Instant.parse("2026-11-26T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        void avanzar(final Duration duracion) {
            ahora = ahora.plus(duracion);
        }
    }

    private RedisCacheConsultas cacheConReloj(final RelojMovil reloj) {
        return new RedisCacheConsultas(redis, mapper, "asistencias-uco", ttl(), new MetricasCache(registry, "redis"), reloj,
                new co.edu.uco.asistenciasuco.infrastructure.cache.DisyuntorRedis(reloj, Duration.ofSeconds(30)));
    }

    @Test
    void tras_una_falla_no_se_llama_a_redis_durante_el_enfriamiento() {
        final RelojMovil reloj = new RelojMovil();
        final RedisCacheConsultas conDisyuntor = cacheConReloj(reloj);
        when(valores.get(CLAVE)).thenThrow(new RedisConnectionFailureException("caido"));

        assertEquals(List.of(FILA), conDisyuntor.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA)));
        assertEquals(List.of(FILA), conDisyuntor.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA)));
        conDisyuntor.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("u1:materias"));

        verify(valores, org.mockito.Mockito.times(1)).get(CLAVE);
        verify(conjuntos, never()).add(anyString(), any(String[].class));
        assertEquals(3.0, contador("error"));
    }

    @Test
    void las_invalidaciones_con_el_disyuntor_abierto_se_aplican_antes_de_volver_a_leer() {
        final RelojMovil reloj = new RelojMovil();
        final RedisCacheConsultas conDisyuntor = cacheConReloj(reloj);
        when(valores.get(CLAVE)).thenThrow(new RedisConnectionFailureException("caido")).thenReturn(null);
        conDisyuntor.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));

        conDisyuntor.invalidar(RegionCache.PORTAL, "u1:resumen");
        conDisyuntor.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");
        verify(redis, never()).delete(anyString());
        reloj.avanzar(Duration.ofSeconds(31));
        when(conjuntos.members("asistencias-uco:cache:v1:portal:etiqueta:grupo:g1")).thenReturn(Set.of(CLAVE));

        conDisyuntor.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));

        final org.mockito.InOrder orden = org.mockito.Mockito.inOrder(redis, valores);
        orden.verify(redis).delete(List.of(CLAVE, "asistencias-uco:cache:v1:portal:etiqueta:grupo:g1"));
        orden.verify(redis).delete(List.of("asistencias-uco:cache:v1:portal:u1:resumen"));
        orden.verify(valores).get(CLAVE);
        verify(valores, org.mockito.Mockito.times(2)).get(CLAVE);
    }

    @Test
    void si_las_pendientes_no_se_pueden_aplicar_la_lectura_va_a_la_fuente() {
        final RelojMovil reloj = new RelojMovil();
        final RedisCacheConsultas conDisyuntor = cacheConReloj(reloj);
        doThrow(new RedisConnectionFailureException("caido")).when(redis).delete(anyString());
        conDisyuntor.invalidar(RegionCache.PORTAL, "u1:materias");
        reloj.avanzar(Duration.ofSeconds(31));
        doThrow(new RedisConnectionFailureException("caido")).when(redis).delete(org.mockito.ArgumentMatchers.<java.util.Collection<String>>any());

        assertEquals(List.of(FILA), conDisyuntor.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA)));
        verify(valores, never()).get(anyString());
    }

    @Test
    void una_invalidacion_durante_la_carga_borra_el_valor_recien_guardado() {
        when(valores.get(CLAVE)).thenReturn(null);

        cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> {
            cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1"); // llega el evento mientras se lee la BD
            return List.of(FILA);
        });

        final org.mockito.InOrder orden = org.mockito.Mockito.inOrder(valores, redis);
        orden.verify(valores).set(org.mockito.ArgumentMatchers.eq(CLAVE), anyString(), org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(60)));
        orden.verify(redis).delete(CLAVE);
    }

    @Test
    void sin_invalidaciones_durante_la_carga_el_valor_se_conserva() {
        when(valores.get(CLAVE)).thenReturn(null);

        cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of(FILA));

        verify(redis, never()).delete(CLAVE);
    }
}
