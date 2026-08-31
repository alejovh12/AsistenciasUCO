package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class TipoIdentificacionRepositoryRowMapper
        implements RowMapper<TipoIdentificacionRepositoryProjection> {

    @Override
    public TipoIdentificacionRepositoryProjection mapRow(final ResultSet resultSet, final int rowNum)
            throws SQLException {
        return new TipoIdentificacionRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                resultSet.getString("tipoIdentificacion"),
                resultSet.getString("nombre")
        );
    }
}
