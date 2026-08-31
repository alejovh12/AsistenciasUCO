package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class EstudianteResumenRepositoryRowMapper implements RowMapper<EstudianteResumenRepositoryProjection> {

    @Override
    public EstudianteResumenRepositoryProjection mapRow(final ResultSet resultSet, final int rowNum)
            throws SQLException {
        return new EstudianteResumenRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                JdbcValueMapper.toUuid(resultSet.getObject("idUsuario")),
                JdbcValueMapper.toUuid(resultSet.getObject("tipoIdentificacionId")),
                JdbcValueMapper.toInteger(resultSet.getObject("numeroIdentificacion")),
                resultSet.getString("primerApellido"),
                resultSet.getString("segundoApellido"),
                resultSet.getString("primerNombre"),
                resultSet.getString("segundoNombre"),
                resultSet.getString("nombreCompleto"),
                resultSet.getString("correo"),
                JdbcValueMapper.toBoolean(resultSet.getObject("estaActivoUsuario"))
        );
    }
}
