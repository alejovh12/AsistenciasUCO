package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AsignaturaDocenteSqlServerAdapter implements AsignaturaDocenteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public AsignaturaDocenteSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<AsignaturaDocenteProjection> consultarAsignaturasDocente(final UUID idDocente) {
        return jdbcOperations.query("""
                SELECT DISTINCT idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, idPrograma, nombrePrograma
                FROM dbo.uv_docente
                WHERE id = :idDocente
                ORDER BY nombreAsignatura, nombreGrupo
                """, new MapSqlParameterSource("idDocente", idDocente), (rs, rowNum) -> new AsignaturaDocenteProjection(
                JdbcValueMapper.toUuid(rs.getObject("idAsignatura")),
                JdbcValueMapper.toString(rs.getObject("nombreAsignatura")),
                JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcValueMapper.toString(rs.getObject("nombreGrupo")),
                JdbcValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcValueMapper.toString(rs.getObject("nombrePrograma"))
        ));
    }
}
