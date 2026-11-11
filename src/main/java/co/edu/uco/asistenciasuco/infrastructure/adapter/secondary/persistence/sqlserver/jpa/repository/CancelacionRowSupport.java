package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CULM-001 C.1: conversiones de filas nativas y construccion de {@code EXEC} sin parametros nulos (evita el binding de nulos sin tipo). */
final class CancelacionRowSupport {

    private CancelacionRowSupport() {
    }

    /**
     * Copia del mapa sin las entradas nulas: es lo unico que se puede enlazar cuando el {@code EXEC} se construyo con
     * {@link #exec(String, Map)}, porque un parametro ausente del SQL no admite {@code setParameter} (el job de resolutores
     * fallaba en cada ejecucion con el lote nulo).
     */
    static Map<String, Object> sinNulos(final Map<String, Object> parametros) {
        final Map<String, Object> vinculables = new LinkedHashMap<>();
        parametros.forEach((nombre, valor) -> {
            if (valor != null) {
                vinculables.put(nombre, valor);
            }
        });
        return vinculables;
    }

    /** {@code EXEC dbo.proc @a = :a, ...} solo con los parametros no nulos; los SP tienen valor por defecto para el resto. */
    static String exec(final String procedimiento, final Map<String, Object> parametros) {
        final StringBuilder sql = new StringBuilder("EXEC dbo.").append(procedimiento);
        String separador = " ";
        for (final Map.Entry<String, Object> parametro : parametros.entrySet()) {
            if (parametro.getValue() != null) {
                sql.append(separador).append('@').append(parametro.getKey()).append(" = :").append(parametro.getKey());
                separador = ", ";
            }
        }
        return sql.toString();
    }

    static List<?> ejecutar(final EntityManager entityManager, final String procedimiento, final Map<String, Object> parametros) {
        final Query query = entityManager.createNativeQuery(exec(procedimiento, parametros));
        parametros.forEach((nombre, valor) -> {
            if (valor != null) {
                query.setParameter(nombre, valor);
            }
        });
        return query.getResultList();
    }

    static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    static UUID uuid(final Object valor) {
        return valor == null ? null : NativeRowValues.uuid(valor);
    }

    static boolean bit(final Object valor) {
        return valor != null && NativeRowValues.bit(valor);
    }

    static int entero(final Object valor) {
        return valor == null ? 0 : NativeRowValues.entero(valor);
    }

    static BigDecimal decimal(final Object valor) {
        return switch (valor) {
            case null -> null;
            case BigDecimal decimal -> decimal;
            case Number numero -> new BigDecimal(numero.toString());
            default -> new BigDecimal(String.valueOf(valor));
        };
    }

    /** DATETIME2 en UTC sin zona. */
    static Instant instante(final Object valor) {
        final java.time.LocalDateTime local = SesionCancelacionJpaRepository.fechaHora(valor);
        return local == null ? null : local.toInstant(java.time.ZoneOffset.UTC);
    }
}
