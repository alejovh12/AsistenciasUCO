package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.TipoIdentificacionRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador SQL Server para consultar la vista de tipos de identificacion.
 */
public final class TipoIdentificacionRepositorySqlServerAdapter implements TipoIdentificacionRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(TipoIdentificacionRepositorySqlServerAdapter.class);
    private static final String OPERATION_CONSULTAR_TIPOS_IDENTIFICACION = "consultarTiposIdentificacion";

    static final String SQL_CONSULTAR_TODOS = """
            SELECT
                id,
                tipoIdentificacion,
                nombre
            FROM dbo.uv_tipo_identificacion
            ORDER BY tipoIdentificacion
            """;

    private final QueryExecutor queryExecutor;

    public TipoIdentificacionRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("El JdbcTemplate para consultar tipos de identificacion es obligatorio.");
        }
        this.queryExecutor = sql -> jdbcTemplate.query(sql, TipoIdentificacionRepositorySqlServerAdapter::mapRow);
    }

    TipoIdentificacionRepositorySqlServerAdapter(final QueryExecutor queryExecutor) {
        if (ObjectHelper.isNull(queryExecutor)) {
            throw new CrosscuttingException("El ejecutor de consulta de tipos de identificacion es obligatorio.");
        }
        this.queryExecutor = queryExecutor;
    }

    @Override
    public List<TipoIdentificacionRepositoryEntity> consultarTiposIdentificacion() {
        try {
            return queryExecutor.query(SQL_CONSULTAR_TODOS);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_CONSULTAR_TIPOS_IDENTIFICACION,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException("No fue posible consultar los tipos de identificacion.", exception);
        }
    }

    static TipoIdentificacionRepositoryEntity mapRow(final ResultSet resultSet, final int rowNumber)
            throws SQLException {
        return new TipoIdentificacionRepositoryEntity(
                toUuid(resultSet.getObject("id")),
                resultSet.getString("tipoIdentificacion"),
                resultSet.getString("nombre")
        );
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return ObjectHelper.isNull(value) ? null : UUID.fromString(String.valueOf(value));
    }

    interface QueryExecutor {

        List<TipoIdentificacionRepositoryEntity> query(String sql);
    }
}
