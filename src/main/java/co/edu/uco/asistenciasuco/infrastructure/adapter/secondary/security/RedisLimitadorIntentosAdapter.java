package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * REDIS-001: limite de intentos por minuto compartido entre replicas (ventana fija de un minuto en Redis).
 *
 * <p>Cada intento hace {@code INCR <prefijo>:limite:v1:<hash de la clave>:<minuto>}; el primero de la ventana fija su
 * vencimiento. Se permite mientras el contador no supere el limite. La clave se guarda como hash (nunca el PIN ni el
 * usuario en claro). Si Redis no responde, decide el limitador en memoria de la replica (degradacion controlada): el
 * registro por QR nunca falla por la cache.</p>
 */
public final class RedisLimitadorIntentosAdapter implements LimitadorIntentosPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLimitadorIntentosAdapter.class);
    static final Duration VENCIMIENTO_VENTANA = Duration.ofSeconds(120);

    private final StringRedisTemplate redis;
    private final String prefijo;
    private final LimitadorIntentosPort respaldo;
    private final Clock clock;

    public RedisLimitadorIntentosAdapter(final StringRedisTemplate redis, final String prefijo,
                                         final LimitadorIntentosPort respaldo, final Clock clock) {
        this.redis = Objects.requireNonNull(redis, "StringRedisTemplate es obligatorio.");
        this.prefijo = Objects.requireNonNull(prefijo, "El prefijo de claves es obligatorio.");
        this.respaldo = Objects.requireNonNull(respaldo, "El limitador de respaldo es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public boolean permitir(final String clave, final int limitePorMinuto) {
        final int limite = Math.max(1, limitePorMinuto);
        final long minuto = clock.millis() / 60_000L;
        final String claveRedis = prefijo + ":limite:v1:" + hash(clave) + ":" + minuto;
        try {
            final Long intentos = redis.opsForValue().increment(claveRedis);
            if (intentos == null) {
                return respaldo.permitir(clave, limite);
            }
            if (intentos == 1L) {
                redis.expire(claveRedis, VENCIMIENTO_VENTANA);
            }
            return intentos <= limite;
        } catch (final RuntimeException exception) {
            LOGGER.debug("Limitador Redis no disponible; decide el limitador local. causa={}", exception.getClass().getSimpleName());
            return respaldo.permitir(clave, limite);
        }
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
