package co.edu.uco.asistenciasuco.infrastructure.cache.redis;

import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * REDIS-001: proveedor {@code redis}: cache compartida por todas las replicas (cache-aside sobre Redis).
 *
 * <ul>
 *   <li>Clave: {@code <prefijo>:cache:v1:<region>:<clave>}; valor: JSON del resultado con el TTL de la region
 *       ({@code SET ... EX}).</li>
 *   <li>Etiquetas: {@code <prefijo>:cache:v1:<region>:etiqueta:<etiqueta>} es un SET con las claves Redis asociadas
 *       ({@code SADD} + {@code EXPIRE} con el TTL de la region). Invalidar la etiqueta borra esas claves y el SET.</li>
 *   <li>Estampida: si muchas peticiones de la misma replica piden a la vez una clave ausente, solo una va a la BD y las
 *       demas esperan su resultado.</li>
 *   <li>Degradacion: si Redis no responde (caido, timeout) o el valor guardado no se puede leer, la consulta se atiende
 *       desde la BD y se cuenta como {@code error}; la peticion nunca falla por la cache. El aviso en el log se limita a
 *       uno por minuto para no inundarlo con Redis caido.</li>
 * </ul>
 */
public final class RedisCacheConsultas implements CacheConsultas {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheConsultas.class);
    static final Duration INTERVALO_AVISO = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final JsonMapper mapper;
    private final String prefijo;
    private final Map<RegionCache, Duration> ttlPorRegion;
    private final MetricasCache metricas;
    private final Clock clock;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> cargasEnCurso = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> ultimoAviso = new AtomicReference<>(Instant.EPOCH);

    public RedisCacheConsultas(final StringRedisTemplate redis, final JsonMapper mapper, final String prefijo,
                               final Map<RegionCache, Duration> ttlPorRegion, final MetricasCache metricas, final Clock clock) {
        this.redis = Objects.requireNonNull(redis, "StringRedisTemplate es obligatorio.");
        this.mapper = Objects.requireNonNull(mapper, "JsonMapper es obligatorio.");
        this.prefijo = Objects.requireNonNull(prefijo, "El prefijo de claves es obligatorio.");
        this.ttlPorRegion = new EnumMap<>(Objects.requireNonNull(ttlPorRegion, "Los TTL por region son obligatorios."));
        for (final RegionCache region : RegionCache.values()) {
            Objects.requireNonNull(this.ttlPorRegion.get(region), "Falta el TTL de la region " + region);
        }
        this.metricas = Objects.requireNonNull(metricas, "MetricasCache es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public <T> T obtener(final RegionCache region, final String clave, final TypeReference<T> tipo, final Supplier<T> cargador) {
        Objects.requireNonNull(cargador, "El cargador es obligatorio.");
        final String claveRedis = claveRedis(region, clave);
        final String guardado;
        try {
            guardado = redis.opsForValue().get(claveRedis);
        } catch (final RuntimeException exception) {
            degradar(region, "leer", exception);
            return cargador.get();
        }
        if (guardado != null) {
            final T valor = leer(region, claveRedis, guardado, tipo);
            if (valor != null) {
                metricas.acierto(region);
                return valor;
            }
        }
        metricas.fallo(region);
        return cargarUnaVez(region, claveRedis, cargador);
    }

    @SuppressWarnings("unchecked")
    private <T> T cargarUnaVez(final RegionCache region, final String claveRedis, final Supplier<T> cargador) {
        final CompletableFuture<Object> propia = new CompletableFuture<>();
        final CompletableFuture<Object> enCurso = cargasEnCurso.putIfAbsent(claveRedis, propia);
        if (enCurso != null) {
            try {
                return (T) enCurso.join();
            } catch (final CompletionException exception) {
                if (exception.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw exception;
            }
        }
        try {
            final T valor = cargador.get();
            guardar(region, claveRedis, valor);
            propia.complete(valor);
            return valor;
        } catch (final RuntimeException exception) {
            propia.completeExceptionally(exception);
            throw exception;
        } finally {
            cargasEnCurso.remove(claveRedis, propia);
        }
    }

    private <T> T leer(final RegionCache region, final String claveRedis, final String json, final TypeReference<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (final RuntimeException exception) {
            // Valor de una version anterior del DTO o corrupto: se descarta y se recarga desde la BD.
            LOGGER.debug("Valor de cache ilegible en la region {}; se descarta.", region.nombre());
            invalidarClave(region, claveRedis);
            return null;
        }
    }

    private void guardar(final RegionCache region, final String claveRedis, final Object valor) {
        if (valor == null) {
            return;
        }
        try {
            redis.opsForValue().set(claveRedis, mapper.writeValueAsString(valor), ttlPorRegion.get(region));
        } catch (final RuntimeException exception) {
            degradar(region, "guardar", exception);
        }
    }

    @Override
    public void invalidar(final RegionCache region, final String clave) {
        invalidarClave(region, claveRedis(region, clave));
    }

    private void invalidarClave(final RegionCache region, final String claveRedis) {
        try {
            redis.delete(claveRedis);
        } catch (final RuntimeException exception) {
            degradar(region, "invalidar", exception);
        }
    }

    @Override
    public void etiquetar(final RegionCache region, final Collection<String> etiquetas, final Collection<String> claves) {
        if (etiquetas == null || claves == null || claves.isEmpty()) {
            return;
        }
        final String[] miembros = claves.stream().map(clave -> claveRedis(region, clave)).toArray(String[]::new);
        try {
            for (final String etiqueta : etiquetas) {
                if (etiqueta != null) {
                    final String claveEtiqueta = claveEtiqueta(region, etiqueta);
                    redis.opsForSet().add(claveEtiqueta, miembros);
                    redis.expire(claveEtiqueta, ttlPorRegion.get(region));
                }
            }
        } catch (final RuntimeException exception) {
            degradar(region, "etiquetar", exception);
        }
    }

    @Override
    public void invalidarEtiqueta(final RegionCache region, final String etiqueta) {
        if (etiqueta == null) {
            return;
        }
        final String claveEtiqueta = claveEtiqueta(region, etiqueta);
        try {
            final Set<String> miembros = redis.opsForSet().members(claveEtiqueta);
            final List<String> borrar = new ArrayList<>();
            if (miembros != null) {
                borrar.addAll(miembros);
            }
            borrar.add(claveEtiqueta);
            redis.delete(borrar);
        } catch (final RuntimeException exception) {
            degradar(region, "invalidar", exception);
        }
    }

    private void degradar(final RegionCache region, final String operacion, final RuntimeException exception) {
        metricas.error(region);
        final Instant ahora = clock.instant();
        final Instant anterior = ultimoAviso.get();
        if (!ahora.isBefore(anterior.plus(INTERVALO_AVISO)) && ultimoAviso.compareAndSet(anterior, ahora)) {
            LOGGER.warn("Cache Redis no disponible al {} (region {}); la consulta se atiende desde la BD. causa={}",
                    operacion, region.nombre(), exception.getClass().getSimpleName());
        }
    }

    String claveRedis(final RegionCache region, final String clave) {
        return prefijo + ":cache:v1:" + region.nombre() + ":" + clave;
    }

    String claveEtiqueta(final RegionCache region, final String etiqueta) {
        return prefijo + ":cache:v1:" + region.nombre() + ":etiqueta:" + etiqueta;
    }

    @Override
    public String proveedor() {
        return "redis";
    }
}
