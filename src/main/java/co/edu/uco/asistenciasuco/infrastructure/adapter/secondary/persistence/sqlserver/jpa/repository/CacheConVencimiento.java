package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria con vencimiento para los catálogos SQL (parámetros y mensajes).
 *
 * <p>VAL-oleada7 (F12-06 / REA-03): con varias réplicas, la invalidación explícita solo llega a la réplica que atendió la
 * edición; el vencimiento hace que las demás tomen el valor nuevo a más tardar en un TTL, sin bus entre réplicas.</p>
 */
final class CacheConVencimiento<K, V> {

    static final Duration TTL_POR_DEFECTO = Duration.ofSeconds(60);
    static final Duration TTL_MAXIMO = Duration.ofHours(24);

    private record Entrada<V>(V valor, Instant vence) { }

    private final Map<K, Entrada<V>> entradas = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final Duration ttl;

    CacheConVencimiento(final Clock reloj, final Duration ttl) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj de la cache es obligatorio.");
        this.ttl = Objects.requireNonNull(ttl, "El TTL de la cache es obligatorio.");
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(TTL_MAXIMO) > 0) {
            throw new IllegalArgumentException("El TTL de la cache de catalogos debe ser mayor que cero y no mayor que "
                    + TTL_MAXIMO + ": " + ttl + ".");
        }
    }

    Optional<V> obtener(final K clave) {
        final Entrada<V> entrada = entradas.get(clave);
        if (entrada == null) {
            return Optional.empty();
        }
        if (!reloj.instant().isBefore(entrada.vence())) {
            entradas.remove(clave, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.valor());
    }

    void guardar(final K clave, final V valor) {
        entradas.put(clave, new Entrada<>(valor, reloj.instant().plus(ttl)));
    }

    void invalidar(final K clave) {
        entradas.remove(clave);
    }

    void invalidarTodo() {
        entradas.clear();
    }
}
