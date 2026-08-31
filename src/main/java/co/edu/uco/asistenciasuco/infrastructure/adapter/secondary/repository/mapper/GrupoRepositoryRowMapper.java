package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class GrupoRepositoryRowMapper implements RowMapper<GrupoRepositoryProjection> {

    @Override
    public GrupoRepositoryProjection mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
        return new GrupoRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                resultSet.getString("codigo"),
                resultSet.getString("nombre"),
                JdbcValueMapper.toUuid(resultSet.getObject("idAsignatura")),
                resultSet.getString("nombreAsignatura"),
                JdbcValueMapper.toUuid(resultSet.getObject("idDocente")),
                JdbcValueMapper.toInteger(resultSet.getObject("capacidadMaximaPermitida")),
                JdbcValueMapper.toInteger(resultSet.getObject("estudiantesActivos")),
                JdbcValueMapper.toInteger(resultSet.getObject("cuposDisponibles")),
                JdbcValueMapper.toBoolean(resultSet.getObject("grupoEstaHablitado")),
                JdbcValueMapper.toLocalDate(resultSet.getObject("fechaInicioPeriodoAcademico")),
                JdbcValueMapper.toLocalDate(resultSet.getObject("fechaFinPeriodoAcademico"))
        );
    }
}
