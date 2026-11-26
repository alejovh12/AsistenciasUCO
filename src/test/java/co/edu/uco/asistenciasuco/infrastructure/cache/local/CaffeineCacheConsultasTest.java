package co.edu.uco.asistenciasuco.infrastructure.cache.local;

import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.SinCacheConsultas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaffeineCacheConsultasTest {

    private static final TypeReference<List<String>> TIPO = new TypeReference<>() { };

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CaffeineCacheConsultas cache = new CaffeineCacheConsultas(ttl(Duration.ofMinutes(1)), 100,
            new MetricasCache(registry, "local"));

    static Map<RegionCache, Duration> ttl(final Duration valor) {
        final Map<RegionCache, Duration> mapa = new EnumMap<>(RegionCache.class);
        for (final RegionCache region : RegionCache.values()) {
            mapa.put(region, valor);
        }
        return mapa;
    }

    private double contador(final String region, final String resultado) {
        return registry.get("asistencias.cache.consultas").tag("region", region).tag("resultado", resultado).counter().count();
    }

    @Test
    void la_segunda_consulta_de_la_misma_clave_no_vuelve_a_la_fuente() {
        final AtomicInteger cargas = new AtomicInteger();

        final List<String> primera = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of("m" + cargas.incrementAndGet()));
        final List<String> segunda = cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of("m" + cargas.incrementAndGet()));

        assertEquals(List.of("m1"), primera);
        assertEquals(List.of("m1"), segunda);
        assertEquals(1, cargas.get());
        assertEquals(1.0, contador("portal", "fallo"));
        assertEquals(1.0, contador("portal", "acierto"));
        assertEquals("local", cache.proveedor());
    }

    @Test
    void las_regiones_y_las_claves_no_se_mezclan() {
        cache.obtener(RegionCache.PORTAL, "k", TIPO, () -> List.of("portal"));
        cache.obtener(RegionCache.INDICADORES, "k", TIPO, () -> List.of("indicadores"));

        assertEquals(List.of("portal"), cache.obtener(RegionCache.PORTAL, "k", TIPO, () -> List.of("otro")));
        assertEquals(List.of("indicadores"), cache.obtener(RegionCache.INDICADORES, "k", TIPO, () -> List.of("otro")));
        assertEquals(List.of("u2"), cache.obtener(RegionCache.PORTAL, "k2", TIPO, () -> List.of("u2")));
    }

    @Test
    void invalidar_obliga_a_recargar_desde_la_fuente() {
        cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO, () -> List.of("viejo"));

        cache.invalidar(RegionCache.PORTAL, "u1:resumen");

        assertEquals(List.of("nuevo"), cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO, () -> List.of("nuevo")));
    }

    @Test
    void un_resultado_nulo_no_se_guarda() {
        final AtomicInteger cargas = new AtomicInteger();

        assertNull(cache.obtener(RegionCache.CATALOGOS, "vacio", TIPO, () -> {
            cargas.incrementAndGet();
            return null;
        }));
        cache.obtener(RegionCache.CATALOGOS, "vacio", TIPO, () -> {
            cargas.incrementAndGet();
            return null;
        });

        assertEquals(2, cargas.get());
    }

    @Test
    void el_valor_vence_con_el_ttl_de_la_region() throws InterruptedException {
        final CaffeineCacheConsultas corta = new CaffeineCacheConsultas(ttl(Duration.ofMillis(50)), 10,
                new MetricasCache(new SimpleMeterRegistry(), "local"));
        corta.obtener(RegionCache.INDICADORES, "kpi", TIPO, () -> List.of("viejo"));

        Thread.sleep(120);

        assertEquals(List.of("nuevo"), corta.obtener(RegionCache.INDICADORES, "kpi", TIPO, () -> List.of("nuevo")));
    }

    @Test
    void una_falla_de_la_fuente_se_propaga_y_no_se_guarda() {
        assertThrows(IllegalStateException.class, () -> cache.obtener(RegionCache.PORTAL, "falla", TIPO, () -> {
            throw new IllegalStateException("bd");
        }));

        assertEquals(List.of("ok"), cache.obtener(RegionCache.PORTAL, "falla", TIPO, () -> List.of("ok")));
    }

    @Test
    void falta_el_ttl_de_una_region_falla_al_construir() {
        final Map<RegionCache, Duration> incompleto = new EnumMap<>(RegionCache.class);
        incompleto.put(RegionCache.PORTAL, Duration.ofSeconds(1));
        final MetricasCache metricas = new MetricasCache(new SimpleMeterRegistry(), "local");

        assertThrows(NullPointerException.class, () -> new CaffeineCacheConsultas(incompleto, 10, metricas));
    }

    @Test
    void sin_cache_siempre_va_a_la_fuente() {
        final SinCacheConsultas sinCache = new SinCacheConsultas();
        final AtomicInteger cargas = new AtomicInteger();

        sinCache.obtener(RegionCache.PORTAL, "k", TIPO, () -> List.of("a" + cargas.incrementAndGet()));
        sinCache.obtener(RegionCache.PORTAL, "k", TIPO, () -> List.of("a" + cargas.incrementAndGet()));
        sinCache.invalidar(RegionCache.PORTAL, "k");
        sinCache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("k"));
        sinCache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");

        assertEquals(2, cargas.get());
        assertEquals("none", sinCache.proveedor());
    }

    @Test
    void invalidar_una_etiqueta_borra_solo_sus_claves() {
        final AtomicInteger cargas = new AtomicInteger();
        cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of("m" + cargas.incrementAndGet()));
        cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO, () -> List.of("r" + cargas.incrementAndGet()));
        cache.obtener(RegionCache.PORTAL, "u2:materias", TIPO, () -> List.of("x" + cargas.incrementAndGet()));
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1", "grupo:g2"), List.of("u1:materias", "u1:resumen"));
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g1"), List.of("u3:materias"));
        cache.etiquetar(RegionCache.PORTAL, null, List.of("u2:materias"));
        cache.etiquetar(RegionCache.PORTAL, List.of("grupo:g9"), List.of());
        cache.etiquetar(RegionCache.PORTAL, java.util.Arrays.asList((String) null), List.of("u2:materias"));
        assertEquals(3, cargas.get());

        cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");
        cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:inexistente");
        cache.invalidarEtiqueta(RegionCache.PORTAL, null);
        cache.obtener(RegionCache.PORTAL, "u1:materias", TIPO, () -> List.of("m" + cargas.incrementAndGet()));
        cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO, () -> List.of("r" + cargas.incrementAndGet()));
        cache.obtener(RegionCache.PORTAL, "u2:materias", TIPO, () -> List.of("x" + cargas.incrementAndGet()));

        assertEquals(5, cargas.get());
    }

    @Test
    void una_invalidacion_durante_la_carga_no_deja_el_valor_en_la_cache() {
        final AtomicInteger cargas = new AtomicInteger();

        cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO, () -> {
            // Llega el evento del grupo mientras se lee la BD (la etiqueta aun no incluye esta clave).
            cache.invalidarEtiqueta(RegionCache.PORTAL, "grupo:g1");
            return List.of("viejo" + cargas.incrementAndGet());
        });

        assertEquals(List.of("nuevo2"), cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO,
                () -> List.of("nuevo" + cargas.incrementAndGet())));
        assertEquals(List.of("nuevo2"), cache.obtener(RegionCache.PORTAL, "u1:resumen", TIPO,
                () -> List.of("otro" + cargas.incrementAndGet())), "sin invalidaciones el valor se conserva");
    }
}
