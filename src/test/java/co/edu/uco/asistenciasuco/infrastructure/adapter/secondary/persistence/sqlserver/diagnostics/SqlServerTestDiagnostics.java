package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.diagnostics;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Diagnostico tecnico no destructivo de la conexion SQL Server usado solo por tests.
 */
final class SqlServerTestDiagnostics {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlServerTestDiagnostics.class);
    private static final String OPERATION_CONNECTION_IS_VALID = "databaseDiagnostics.connectionIsValid";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    SqlServerTestDiagnostics(final DataSource dataSource, final JdbcTemplate jdbcTemplate) {
        this.dataSource = Objects.requireNonNull(dataSource, "El DataSource para diagnostico de base de datos es obligatorio.");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para diagnostico de base de datos es obligatorio.");
    }

    boolean connectionIsValid() {
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

    Integer selectOne() {
        return jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }

    String currentDatabaseName() {
        return jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
    }

    String connectedUserName() {
        return jdbcTemplate.queryForObject("SELECT SUSER_SNAME()", String.class);
    }

    Map<String, Object> serverInfo() {
        return jdbcTemplate.queryForMap("""
                SELECT
                    SERVERPROPERTY('ProductVersion') AS productVersion,
                    SERVERPROPERTY('ProductLevel') AS productLevel,
                    SERVERPROPERTY('Edition') AS edition
                """);
    }

    boolean viewExists(final String viewName) {
        final Integer objectId = jdbcTemplate.queryForObject(
                "SELECT OBJECT_ID(?, 'V')",
                Integer.class,
                viewName
        );
        return ObjectHelper.isNotNull(objectId);
    }

    List<TipoIdentificacionProbeRow> firstTipoIdentificacionRows() {
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

    record TipoIdentificacionProbeRow(UUID id, String tipoIdentificacion, String nombre) {
    }
}
