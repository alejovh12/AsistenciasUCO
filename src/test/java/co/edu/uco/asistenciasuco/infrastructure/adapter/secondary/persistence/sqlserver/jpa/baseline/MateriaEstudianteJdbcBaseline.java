package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MateriaEstudianteJdbcBaseline implements MateriaEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public MateriaEstudianteJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
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
                JdbcBaselineValueMapper.toUuid(rs.getObject("idAsignatura")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreAsignatura")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreGrupo"))
        ));
    }
}


