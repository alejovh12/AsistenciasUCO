package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class DocenteAsignacionAcademicaRepositoryRowMapper
        implements RowMapper<DocenteAsignacionAcademicaRepositoryProjection> {

    @Override
    public DocenteAsignacionAcademicaRepositoryProjection mapRow(
            final ResultSet resultSet,
            final int rowNum
    ) throws SQLException {
        return new DocenteAsignacionAcademicaRepositoryProjection(
                JdbcValueMapper.toUuid(resultSet.getObject("id")),
                JdbcValueMapper.toUuid(resultSet.getObject("idUsuario")),
                JdbcValueMapper.toInteger(resultSet.getObject("numeroIdentificacion")),
                resultSet.getString("nombreCompleto"),
                JdbcValueMapper.toBoolean(resultSet.getObject("estaActivoUsuario")),
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
                resultSet.getString("nombreGrupo"),
                JdbcValueMapper.toUuid(resultSet.getObject("idPerfil")),
                resultSet.getString("codigoPerfil"),
                resultSet.getString("nombrePerfil"),
                JdbcValueMapper.toInteger(resultSet.getObject("estaActivoDocente")),
                resultSet.getString("estaActivoTextoDocente")
        );
    }
}
