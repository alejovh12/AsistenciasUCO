package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.TipoIdentificacionRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador SQL Server para consultar la vista de tipos de identificacion.
 */
public final class TipoIdentificacionRepositorySqlServerAdapter implements TipoIdentificacionRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(TipoIdentificacionRepositorySqlServerAdapter.class);
    private static final RowMapper<TipoIdentificacionRepositoryProjection> TIPO_IDENTIFICACION_ROW_MAPPER =
            new TipoIdentificacionRepositoryRowMapper();
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
        Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para consultar tipos de identificacion es obligatorio.");
        this.queryExecutor = sql -> jdbcTemplate.query(sql, TIPO_IDENTIFICACION_ROW_MAPPER);
    }

    TipoIdentificacionRepositorySqlServerAdapter(final QueryExecutor queryExecutor) {
        this.queryExecutor = Objects.requireNonNull(
                queryExecutor,
                "El ejecutor de consulta de tipos de identificacion es obligatorio."
        );
    }

    @Override
    public List<TipoIdentificacionRepositoryProjection> consultarTiposIdentificacion() {
        try {
            return queryExecutor.query(SQL_CONSULTAR_TODOS);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_TIPOS_IDENTIFICACION,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los tipos de identificacion.", exception);
        }
    }

    interface QueryExecutor {

        List<TipoIdentificacionRepositoryProjection> query(String sql);
    }
}
