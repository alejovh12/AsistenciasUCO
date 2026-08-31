package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteContextoAcademicoRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class EstudianteContextoAcademicoRepositoryRowMapper
        implements RowMapper<EstudianteContextoAcademicoRepositoryProjection> {

    @Override
    public EstudianteContextoAcademicoRepositoryProjection mapRow(
            final ResultSet resultSet,
            final int rowNum
    ) throws SQLException {
        return new EstudianteContextoAcademicoRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("idInstitucion")),
                resultSet.getString("nombreInstitucion"),
                JdbcValueMapper.toUuid(resultSet.getObject("idFacultad")),
                resultSet.getString("nombreFacultad"),
                JdbcValueMapper.toUuid(resultSet.getObject("idPrograma")),
                resultSet.getString("nombrePrograma"),
                JdbcValueMapper.toUuid(resultSet.getObject("idPlanEstudio")),
                resultSet.getString("inpPlanEstudio"),
                JdbcValueMapper.toUuid(resultSet.getObject("idAsignatura")),
                resultSet.getString("nombreAsignatura"),
                JdbcValueMapper.toUuid(resultSet.getObject("idGrupo")),
                resultSet.getString("nombreGrupo")
        );
    }
}
