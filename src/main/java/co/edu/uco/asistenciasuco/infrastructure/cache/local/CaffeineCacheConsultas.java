package co.edu.uco.asistenciasuco.infrastructure.cache.local;

import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * REDIS-001: proveedor {@code local}: cache en memoria de cada replica (Caffeine), con un TTL por region.
 *
 * <p>Caffeine carga una sola vez por clave aunque lleguen muchas peticiones a la vez (las demas esperan ese resultado),
 * lo que evita la estampida sobre la BD. Con varias replicas cada una tiene su copia: un dato puede diferir entre
 * replicas hasta el TTL de su region. Para compartirla entre replicas se usa el proveedor {@code redis}.</p>
 */
public final class CaffeineCacheConsultas implements CacheConsultas {

    private final Map<RegionCache, Cache<String, Object>> caches = new EnumMap<>(RegionCache.class);
    /** Indice etiqueta -> claves por region; vence con el mismo TTL (si sobrevive a sus claves, borrarlas no hace nada). */
    private final Map<RegionCache, Cache<String, Set<String>>> etiquetas = new EnumMap<>(RegionCache.class);
    private final MetricasCache metricas;
    /** Code review 2026-11-26: cuenta las invalidaciones; una carga que ve cambiar el contador no deja su valor. */
    private final java.util.concurrent.atomic.AtomicLong generacion = new java.util.concurrent.atomic.AtomicLong();

    public CaffeineCacheConsultas(final Map<RegionCache, Duration> ttlPorRegion, final long maximoPorRegion,
                                  final MetricasCache metricas) {
        Objects.requireNonNull(ttlPorRegion, "Los TTL por region son obligatorios.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasCache es obligatorio.");
        for (final RegionCache region : RegionCache.values()) {
            final Duration ttl = Objects.requireNonNull(ttlPorRegion.get(region), "Falta el TTL de la region " + region);
            caches.put(region, Caffeine.newBuilder().maximumSize(maximoPorRegion).expireAfterWrite(ttl).build());
            etiquetas.put(region, Caffeine.newBuilder().maximumSize(maximoPorRegion).expireAfterWrite(ttl).build());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T obtener(final RegionCache region, final String clave, final TypeReference<T> tipo, final Supplier<T> cargador) {
        Objects.requireNonNull(cargador, "El cargador es obligatorio.");
        final Cache<String, Object> cache = caches.get(region);
        final Object presente = cache.getIfPresent(clave);
        if (presente != null) {
            metricas.acierto(region);
            return (T) presente;
        }
        metricas.fallo(region);
        final long antes = generacion.get();
        final Object valor = cache.get(clave, ignorada -> cargador.get());
        if (generacion.get() != antes) {
            // Hubo una invalidacion durante la carga: el valor pudo leerse antes del cambio. No se deja en la cache.
            cache.invalidate(clave);
        }
        return (T) valor;
    }

    @Override
    public void invalidar(final RegionCache region, final String clave) {
        generacion.incrementAndGet();
        caches.get(region).invalidate(clave);
    }

    @Override
    public void etiquetar(final RegionCache region, final Collection<String> etiquetasClave, final Collection<String> claves) {
        if (etiquetasClave == null || claves == null || claves.isEmpty()) {
            return;
        }
        final Cache<String, Set<String>> indice = etiquetas.get(region);
        for (final String etiqueta : etiquetasClave) {
            if (etiqueta != null) {
                indice.asMap().compute(etiqueta, (ignorada, actuales) -> {
                    final Set<String> conjunto = actuales == null ? ConcurrentHashMap.newKeySet() : actuales;
                    conjunto.addAll(claves);
                    return conjunto;
                });
            }
        }
    }

    @Override
    public void invalidarEtiqueta(final RegionCache region, final String etiqueta) {
        if (etiqueta == null) {
            return;
        }
        generacion.incrementAndGet();
        final Set<String> claves = etiquetas.get(region).asMap().remove(etiqueta);
        if (claves != null) {
            caches.get(region).invalidateAll(claves);
        }
    }

    @Override
    public String proveedor() {
        return "local";
    }
}
