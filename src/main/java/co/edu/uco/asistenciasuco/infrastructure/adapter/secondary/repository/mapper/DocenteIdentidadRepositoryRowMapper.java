package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class DocenteIdentidadRepositoryRowMapper implements RowMapper<DocenteIdentidadRepositoryProjection> {

    @Override
    public DocenteIdentidadRepositoryProjection mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
        return new DocenteIdentidadRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                JdbcValueMapper.toUuid(resultSet.getObject("idUsuario")),
                JdbcValueMapper.toInteger(resultSet.getObject("numeroIdentificacion")),
                resultSet.getString("nombreCompleto"),
                JdbcValueMapper.toBoolean(resultSet.getObject("estaActivoUsuario"))
        );
    }
}
