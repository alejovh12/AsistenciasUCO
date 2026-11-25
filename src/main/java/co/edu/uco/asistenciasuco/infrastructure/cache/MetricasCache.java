package co.edu.uco.asistenciasuco.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * REDIS-001: contadores de la cache de consultas ({@code asistencias_cache_consultas_total{region,resultado}} en
 * Prometheus). {@code resultado} es {@code acierto}, {@code fallo} (se fue a la BD) o {@code error} (la cache no respondio
 * y se degrado a la BD). Cardinalidad cerrada: 3 regiones x 3 resultados.
 */
public final class MetricasCache {

    static final String METRICA = "asistencias.cache.consultas";

    private final Map<RegionCache, Counter> aciertos = new EnumMap<>(RegionCache.class);
    private final Map<RegionCache, Counter> fallos = new EnumMap<>(RegionCache.class);
    private final Map<RegionCache, Counter> errores = new EnumMap<>(RegionCache.class);

    public MetricasCache(final MeterRegistry registry, final String proveedor) {
        Objects.requireNonNull(registry, "MeterRegistry es obligatorio.");
        for (final RegionCache region : RegionCache.values()) {
            aciertos.put(region, contador(registry, proveedor, region, "acierto"));
            fallos.put(region, contador(registry, proveedor, region, "fallo"));
            errores.put(region, contador(registry, proveedor, region, "error"));
        }
    }

    private static Counter contador(final MeterRegistry registry, final String proveedor, final RegionCache region,
                                    final String resultado) {
        return Counter.builder(METRICA)
                .description("Consultas atendidas por la cache (acierto), cargadas de la BD (fallo) o degradadas (error).")
                .tag("proveedor", proveedor)
                .tag("region", region.nombre())
                .tag("resultado", resultado)
                .register(registry);
    }

    public void acierto(final RegionCache region) {
        aciertos.get(region).increment();
    }

    public void fallo(final RegionCache region) {
        fallos.get(region).increment();
    }

    public void error(final RegionCache region) {
        errores.get(region).increment();
    }
}
