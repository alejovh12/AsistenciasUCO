package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapea el unico resultset canonico de los procedimientos almacenados publicos.
 */
public final class CanonicalProcedureResultMapper implements RowMapper<CanonicalProcedureResult> {

    public static final String FIELD_ID_CORRELACION = "idCorrelacion";
    public static final String FIELD_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    public static final String FIELD_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    public static final String FIELD_ESTADO = "estadoResultado";

    @Override
    public CanonicalProcedureResult mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
        return new CanonicalProcedureResult(
                JdbcValueMapper.toUuid(resultSet.getObject(FIELD_ID_CORRELACION)),
                JdbcValueMapper.toString(resultSet.getObject(FIELD_MENSAJE_USUARIO)),
                JdbcValueMapper.toString(resultSet.getObject(FIELD_MENSAJE_TECNICO)),
                JdbcValueMapper.toBoolean(resultSet.getObject(FIELD_ESTADO))
        );
    }
}
