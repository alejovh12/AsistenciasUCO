package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SesionMateriaEstudianteSqlServerAdapter implements SesionMateriaEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public SesionMateriaEstudianteSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<SesionMateriaEstudianteProjection> consultarSesionesMateria(final UUID idEstudiante, final UUID idAsignatura) {
        return jdbcOperations.query("""
                SELECT s.id, s.nombre, s.numero, s.codigo, s.numeroSemana, s.idGrupo, s.codigoGrupo, s.nombreGrupo,
                       s.fechaHoraInicio, s.fechaHoraFin
                FROM dbo.uv_sesion s
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.idGrupo = s.idGrupo
                INNER JOIN dbo.uv_grupo g ON g.id = s.idGrupo
                WHERE eg.idEstudiante = :idEstudiante
                  AND g.idAsignatura = :idAsignatura
                ORDER BY s.numero, s.fechaHoraInicio
                """, new MapSqlParameterSource()
                .addValue("idEstudiante", idEstudiante)
                .addValue("idAsignatura", idAsignatura), (rs, rowNum) -> new SesionMateriaEstudianteProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toString(rs.getObject("nombre")),
                JdbcValueMapper.toInteger(rs.getObject("numero")),
                JdbcValueMapper.toString(rs.getObject("codigo")),
                JdbcValueMapper.toInteger(rs.getObject("numeroSemana")),
                JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcValueMapper.toString(rs.getObject("codigoGrupo")),
                JdbcValueMapper.toString(rs.getObject("nombreGrupo")),
                JdbcValueMapper.toLocalDateTime(rs.getObject("fechaHoraInicio")),
                JdbcValueMapper.toLocalDateTime(rs.getObject("fechaHoraFin"))
        ));
    }
}
