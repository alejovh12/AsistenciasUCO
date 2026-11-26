package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.infrastructure.cache.DisyuntorRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REDIS-001: limite de intentos compartido entre replicas, con la MISMA semantica que el limitador en memoria que
 * reemplaza ({@code SlidingWindowLimitadorIntentosAdapter}): ventana deslizante de 60 s y solo cuentan los intentos
 * aceptados (un rechazo no extiende el bloqueo).
 *
 * <p>Cada clave es un sorted set {@code <prefijo>:limite:v2:<hash de la clave>} con un miembro por intento aceptado
 * (puntaje = instante en ms). Un script Lua hace, de forma atomica, la poda de los intentos vencidos, el conteo, el
 * registro del intento si cabe y el vencimiento de la clave; asi nunca queda una clave sin TTL ni se puede duplicar el
 * limite en el cambio de minuto (hallazgos del code review 2026-11-26). La clave se guarda como hash (nunca el PIN ni el
 * usuario en claro). Si Redis no responde, decide el limitador en memoria de la replica (degradacion controlada): el
 * registro por QR nunca falla por la cache. Comparte el {@link DisyuntorRedis} de la cache: con Redis caido no espera su
 * timeout en cada intento.</p>
 */
public final class RedisLimitadorIntentosAdapter implements LimitadorIntentosPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLimitadorIntentosAdapter.class);
    static final Duration VENTANA = Duration.ofSeconds(60);

    /**
     * KEYS[1] = clave; ARGV[1] = ahora (ms); ARGV[2] = ventana (ms); ARGV[3] = limite; ARGV[4] = miembro unico.
     * Devuelve 1 si el intento se acepta (y queda registrado) o 0 si se rechaza.
     */
    static final RedisScript<Long> SCRIPT = RedisScript.of("""
            local ahora = tonumber(ARGV[1])
            local ventana = tonumber(ARGV[2])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ahora - ventana)
            local aceptado = 0
            if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[3]) then
              redis.call('ZADD', KEYS[1], ahora, ARGV[4])
              aceptado = 1
            end
            redis.call('PEXPIRE', KEYS[1], ventana)
            return aceptado
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String prefijo;
    private final LimitadorIntentosPort respaldo;
    private final Clock clock;
    private final DisyuntorRedis disyuntor;

    public RedisLimitadorIntentosAdapter(final StringRedisTemplate redis, final String prefijo,
                                         final LimitadorIntentosPort respaldo, final Clock clock) {
        this(redis, prefijo, respaldo, clock, new DisyuntorRedis(clock));
    }

    public RedisLimitadorIntentosAdapter(final StringRedisTemplate redis, final String prefijo,
                                         final LimitadorIntentosPort respaldo, final Clock clock,
                                         final DisyuntorRedis disyuntor) {
        this.redis = Objects.requireNonNull(redis, "StringRedisTemplate es obligatorio.");
        this.prefijo = Objects.requireNonNull(prefijo, "El prefijo de claves es obligatorio.");
        this.respaldo = Objects.requireNonNull(respaldo, "El limitador de respaldo es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.disyuntor = Objects.requireNonNull(disyuntor, "DisyuntorRedis es obligatorio.");
    }

    @Override
    public boolean permitir(final String clave, final int limitePorMinuto) {
        final int limite = Math.max(1, limitePorMinuto);
        final long ahora = clock.millis();
        if (!disyuntor.permite()) {
            return respaldo.permitir(clave, limite);
        }
        try {
            final Long aceptado = redis.execute(SCRIPT, List.of(claveRedis(clave)), Long.toString(ahora),
                    Long.toString(VENTANA.toMillis()), Integer.toString(limite), ahora + ":" + UUID.randomUUID());
            if (aceptado == null) {
                return respaldo.permitir(clave, limite);
            }
            disyuntor.registrarExito();
            return aceptado == 1L;
        } catch (final RuntimeException exception) {
            disyuntor.registrarFalla();
            LOGGER.debug("Limitador Redis no disponible; decide el limitador local. causa={}", exception.getClass().getSimpleName());
            return respaldo.permitir(clave, limite);
        }
    }

    /** Clave Redis del contador (visible para diagnostico y pruebas). */
    public String claveRedis(final String clave) {
        return prefijo + ":limite:v2:" + hash(clave);
    }

    static String hash(final String clave) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(clave).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM.", exception);
        }
    }
}
