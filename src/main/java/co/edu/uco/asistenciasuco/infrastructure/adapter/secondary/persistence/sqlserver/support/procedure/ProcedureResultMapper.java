package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;

import java.util.List;
import java.util.UUID;

/**
 * Mapper unico de la fila canonica de un procedimiento almacenado (LB-008 JPA-01).
 *
 * <p>Convierte {@code row[0]} en idCorrelacion, {@code row[1]} en mensajeUsuarioResultado,
 * {@code row[2]} en mensajeTecnicoResultado y {@code row[3]} en estadoResultado (BIT). Valida solo la forma
 * del contrato: cardinalidad exacta 1, 4 columnas, UUID no nulo y BIT compatible. No evalua
 * {@code estadoResultado} ni la correlacion: eso lo hace {@link ProcedureResultValidator}, y un
 * fallo funcional del SP nunca se confunde con una violacion de forma.</p>
 */
public final class ProcedureResultMapper {

    private static final int CANONICAL_COLUMNS = 4;
    private static final String CONTRACT_MESSAGE = "El procedimiento no retorno el contrato canonico esperado.";

    private ProcedureResultMapper() {
    }

    /**
     * @param rows filas retornadas por la consulta nativa del procedimiento (cada fila: {@code Object[]})
     * @return el unico resultado canonico, de forma validada
     * @throws DatabaseOperationException {@code ERR_DB_CANONICAL_CONTRACT} si la forma no cumple el contrato
     */
    public static ProcedureResult mapSingle(final List<?> rows) {
        if (rows == null || rows.size() != 1) {
            throw contractViolation();
        }
        final Object row = rows.getFirst();
        if (!(row instanceof Object[] columns) || columns.length != CANONICAL_COLUMNS) {
            throw contractViolation();
        }
        return new CanonicalProcedureResult(
                toUuid(columns[0]),
                toText(columns[1]),
                toText(columns[2]),
                toBit(columns[3])
        );
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException exception) {
                throw contractViolation(exception);
            }
        }
        throw contractViolation();
    }

    private static String toText(final Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw contractViolation();
    }

    private static boolean toBit(final Object value) {
        if (value instanceof Boolean bit) {
            return bit;
        }
        if (value instanceof Number number && (number.intValue() == 0 || number.intValue() == 1)) {
            return number.intValue() == 1;
        }
        throw contractViolation();
    }

    private static DatabaseOperationException contractViolation() {
        return new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT, CONTRACT_MESSAGE);
    }

    private static DatabaseOperationException contractViolation(final Throwable cause) {
        return new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT, CONTRACT_MESSAGE, cause);
    }
}
