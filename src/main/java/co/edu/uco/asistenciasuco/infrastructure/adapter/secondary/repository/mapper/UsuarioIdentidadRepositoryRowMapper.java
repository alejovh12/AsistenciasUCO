package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class UsuarioIdentidadRepositoryRowMapper implements RowMapper<UsuarioIdentidadRepositoryProjection> {

    @Override
    public UsuarioIdentidadRepositoryProjection mapRow(final ResultSet resultSet, final int rowNum)
            throws SQLException {
        return new UsuarioIdentidadRepositoryProjection(JdbcValueMapper.toUuid(resultSet.getObject("id")));
    }
}
