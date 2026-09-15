package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Ejecuta procedimientos almacenados publicos que retornan el contrato canonico.
 */
public class CanonicalStoredProcedureExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalStoredProcedureExecutor.class);
    private static final CanonicalProcedureResultMapper RESULT_MAPPER = new CanonicalProcedureResultMapper();

    private final NamedParameterJdbcOperations jdbcOperations;

    public CanonicalStoredProcedureExecutor(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(
                jdbcOperations,
                "Las operaciones JDBC nombradas son obligatorias para ejecutar procedimientos."
        );
    }

    public CanonicalProcedureResult execute(
            final String operation,
            final String sql,
            final MapSqlParameterSource parameters
    ) {
        final String normalizedOperation = TextHelper.isNullOrBlank(operation) ? "unknown" : operation;
        final UUID correlationId = CorrelationIdContext.require();
        try {
            final List<CanonicalProcedureResult> results = jdbcOperations.query(
                    Objects.requireNonNull(sql, "El SQL del procedimiento es obligatorio."),
                    Objects.requireNonNull(parameters, "Los parametros del procedimiento son obligatorios."),
                    RESULT_MAPPER
            );
            if (results.size() != 1) {
                throw new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                        "El procedimiento no retorno el contrato canonico esperado."
                );
            }
            final CanonicalProcedureResult result = results.getFirst();
            validateCorrelationId(result, correlationId);
            DbExceptionTranslator.throwIfFailed(
                    result.isEstadoResultado(),
                    result.getMensajeUsuarioResultado(),
                    result.getMensajeTecnicoResultado(),
                    correlationId.toString(),
                    normalizedOperation
            );
            return result;
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    normalizedOperation,
                    correlationId
            );
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible ejecutar el procedimiento almacenado.",
                    exception
            );
        }
    }

    private void validateCorrelationId(final CanonicalProcedureResult result, final UUID contextCorrelationId) {
        final UUID resultCorrelationId = result.getIdCorrelacion();
        if (!Objects.equals(contextCorrelationId, resultCorrelationId)) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "La correlacion retornada por la DB no coincide con la peticion."
            );
        }
    }
}
