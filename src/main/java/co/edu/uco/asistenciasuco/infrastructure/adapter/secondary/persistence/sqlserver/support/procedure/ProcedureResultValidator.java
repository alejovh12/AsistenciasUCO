package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;

import java.util.Objects;
import java.util.UUID;

/**
 * Valida la semántica de un resultado canónico ya construido por {@link ProcedureResultMapper}.
 * La cardinalidad, las columnas y los tipos pertenecen exclusivamente al mapper.
 */
public final class ProcedureResultValidator {

    private ProcedureResultValidator() {
    }

    public static ProcedureResult validate(
            final ProcedureResult result,
            final UUID expectedCorrelationId,
            final String operation
    ) {
        if (result == null) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento no retorno resultado."
            );
        }

        if (!Objects.equals(expectedCorrelationId, result.getIdCorrelacion())) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "La correlacion retornada por la DB no coincide con la peticion."
            );
        }

        DbExceptionTranslator.throwIfFailed(
                result.getEstadoResultado(),
                result.getMensajeUsuarioResultado(),
                result.getMensajeTecnicoResultado(),
                String.valueOf(expectedCorrelationId),
                operation
        );

        return result;
    }
}
