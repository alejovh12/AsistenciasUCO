package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MateriaEstudianteSqlServerAdapter implements MateriaEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public MateriaEstudianteSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<MateriaEstudianteProjection> consultarMateriasEstudiante(final UUID idEstudiante) {
        return jdbcOperations.query("""
                SELECT DISTINCT a.id AS idAsignatura, a.nombre AS nombreAsignatura, g.id AS idGrupo, g.nombre AS nombreGrupo
                FROM dbo.uv_estudiante_grupo eg
                INNER JOIN dbo.uv_grupo g ON g.id = eg.idGrupo
                INNER JOIN dbo.uv_asignatura a ON a.id = g.idAsignatura
                WHERE eg.idEstudiante = :idEstudiante
                ORDER BY a.nombre, g.nombre
                """, new MapSqlParameterSource("idEstudiante", idEstudiante), (rs, rowNum) -> new MateriaEstudianteProjection(
                JdbcValueMapper.toUuid(rs.getObject("idAsignatura")),
                JdbcValueMapper.toString(rs.getObject("nombreAsignatura")),
                JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcValueMapper.toString(rs.getObject("nombreGrupo"))
        ));
    }
}
