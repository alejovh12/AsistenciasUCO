package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * REDIS-001: {@code app.adapters.cache}: proveedor de la cache de consultas ({@code local} por defecto, {@code redis} o
 * {@code none}), TTL por region, tamano maximo por region (solo {@code local}) y prefijo de claves (solo {@code redis}).
 */
@ConfigurationProperties(prefix = "app.adapters.cache")
public record CacheAdapterProperties(Provider provider, Ttl ttl, Long maximoPorRegion, String prefijo) {

    static final Duration MAXIMO_TTL = Duration.ofHours(24);

    public CacheAdapterProperties {
        provider = provider == null ? Provider.LOCAL : provider;
        ttl = ttl == null ? new Ttl(null, null, null) : ttl;
        maximoPorRegion = maximoPorRegion == null || maximoPorRegion < 1 ? 10_000L : maximoPorRegion;
        prefijo = prefijo == null || prefijo.isBlank() ? "asistencias-uco" : prefijo.strip();
    }

    public Map<RegionCache, Duration> ttlPorRegion() {
        final Map<RegionCache, Duration> mapa = new EnumMap<>(RegionCache.class);
        mapa.put(RegionCache.CATALOGOS, ttl.catalogos());
        mapa.put(RegionCache.INDICADORES, ttl.indicadores());
        mapa.put(RegionCache.PORTAL, ttl.portal());
        return mapa;
    }

    public enum Provider {
        LOCAL,
        REDIS,
        NONE
    }

    /** TTL por region; un valor ausente toma el predeterminado y uno fuera de (0, 24 h] falla el arranque. */
    public record Ttl(Duration catalogos, Duration indicadores, Duration portal) {

        public Ttl {
            catalogos = validar("catalogos", catalogos, Duration.ofMinutes(30));
            indicadores = validar("indicadores", indicadores, Duration.ofMinutes(5));
            portal = validar("portal", portal, Duration.ofSeconds(60));
        }

        private static Duration validar(final String nombre, final Duration valor, final Duration predeterminado) {
            if (valor == null) {
                return predeterminado;
            }
            if (valor.isZero() || valor.isNegative() || valor.compareTo(MAXIMO_TTL) > 0) {
                throw new IllegalArgumentException("app.adapters.cache.ttl." + nombre + " debe estar entre 1 s y 24 h.");
            }
            return valor;
        }
    }
}
