package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ejecutor unico de procedimientos almacenados publicos sobre {@link EntityManager} (LB-008 JPA-only).
 *
 * <p>Centraliza el bloque repetido en cada {@code @Repository}: {@code createNativeQuery} + binding de
 * parametros + {@code getResultList} + {@link ProcedureResultMapper} + {@link ProcedureResultValidator},
 * incluida la traduccion de fallos tecnicos de JPA/Hibernate y su logging. Un {@code @Repository} con
 * commands de SP ya no declara su propio metodo {@code ejecutar} ni captura
 * {@code PersistenceException}/{@code IllegalStateException}/{@code IllegalArgumentException} para ese
 * proposito.</p>
 */
@Component
public final class JpaProcedureExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaProcedureExecutor.class);

    private final EntityManager entityManager;

    public JpaProcedureExecutor(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    /**
     * Ejecuta el SP identificado por {@code sql}, mapea la fila canonica y valida su semantica
     * (correlacion y {@code estadoResultado}) antes de devolverla.
     *
     * @param operation     nombre de la operacion para logging/clasificacion de errores
     * @param sql           sentencia {@code EXEC dbo.usp_...} con parametros nombrados
     * @param parameters    parametros a enlazar por nombre
     * @param correlationId correlacion esperada en la fila canonica devuelta por la DB
     */
    public ProcedureResult execute(
            final String operation,
            final String sql,
            final Map<String, ?> parameters,
            final UUID correlationId
    ) {
        final ProcedureResult result = runQuery(operation, sql, parameters);
        return ProcedureResultValidator.validate(result, correlationId, operation);
    }

    private ProcedureResult runQuery(final String operation, final String sql, final Map<String, ?> parameters) {
        try {
            final Query query = entityManager.createNativeQuery(sql);
            parameters.forEach(query::setParameter);
            return ProcedureResultMapper.mapSingle(query.getResultList());
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    operation,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible ejecutar el procedimiento almacenado.",
                    exception
            );
        }
    }
}
