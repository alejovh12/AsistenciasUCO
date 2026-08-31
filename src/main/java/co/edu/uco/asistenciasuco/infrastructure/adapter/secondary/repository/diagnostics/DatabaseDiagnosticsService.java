package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.diagnostics;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

/**
 * Diagnostico tecnico no destructivo de la conexion SQL Server.
 */
@Component
public final class DatabaseDiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDiagnosticsService.class);
    private static final String OPERATION_CONNECTION_IS_VALID = "databaseDiagnostics.connectionIsValid";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseDiagnosticsService(final DataSource dataSource, final JdbcTemplate jdbcTemplate) {
        this.dataSource = Objects.requireNonNull(dataSource, "El DataSource para diagnostico de base de datos es obligatorio.");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para diagnostico de base de datos es obligatorio.");
    }

    public boolean connectionIsValid() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONNECTION_IS_VALID,
                    CorrelationIdContext.getAsString(),
                    exception
            );
            throw new DatabaseOperationException("No fue posible validar la conexion a SQL Server.", exception);
        }
    }

    public Integer selectOne() {
        return jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }

    public String currentDatabaseName() {
        return jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
    }

    public String connectedUserName() {
        return jdbcTemplate.queryForObject("SELECT SUSER_SNAME()", String.class);
    }

    public Map<String, Object> serverInfo() {
        return jdbcTemplate.queryForMap("""
                SELECT
                    SERVERPROPERTY('ProductVersion') AS productVersion,
                    SERVERPROPERTY('ProductLevel') AS productLevel,
                    SERVERPROPERTY('Edition') AS edition
                """);
    }

    public boolean viewExists(final String viewName) {
        final Integer objectId = jdbcTemplate.queryForObject(
                "SELECT OBJECT_ID(?, 'V')",
                Integer.class,
                viewName
        );
        return ObjectHelper.isNotNull(objectId);
    }

    public List<TipoIdentificacionProbeRow> firstTipoIdentificacionRows() {
        return jdbcTemplate.query("""
                SELECT TOP (1)
                    id,
                    tipoIdentificacion,
                    nombre
                FROM dbo.uv_tipo_identificacion
                ORDER BY tipoIdentificacion, id
                """, (resultSet, rowNumber) -> new TipoIdentificacionProbeRow(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                resultSet.getString("tipoIdentificacion"),
                resultSet.getString("nombre")
        ));
    }

    public record TipoIdentificacionProbeRow(UUID id, String tipoIdentificacion, String nombre) {
    }
}
