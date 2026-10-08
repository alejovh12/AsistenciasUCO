package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapea el unico resultset canonico de los procedimientos almacenados publicos.
 */
public final class CanonicalJdbcBaselineResultMapper implements RowMapper<CanonicalProcedureResult> {

    public static final String FIELD_ID_CORRELACION = "idCorrelacion";
    public static final String FIELD_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    public static final String FIELD_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    public static final String FIELD_ESTADO = "estadoResultado";

    @Override
    public CanonicalProcedureResult mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
        return new CanonicalProcedureResult(
                JdbcBaselineValueMapper.toUuid(resultSet.getObject(FIELD_ID_CORRELACION)),
                JdbcBaselineValueMapper.toString(resultSet.getObject(FIELD_MENSAJE_USUARIO)),
                JdbcBaselineValueMapper.toString(resultSet.getObject(FIELD_MENSAJE_TECNICO)),
                JdbcBaselineValueMapper.toBoolean(resultSet.getObject(FIELD_ESTADO))
        );
    }
}
