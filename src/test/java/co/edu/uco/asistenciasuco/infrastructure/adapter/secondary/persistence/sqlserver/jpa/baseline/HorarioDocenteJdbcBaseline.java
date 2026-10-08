package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class HorarioDocenteJdbcBaseline implements HorarioDocenteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public HorarioDocenteJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<HorarioDocenteProjection> consultarHorarioDocente(final UUID idDocente) {
        return jdbcOperations.query("""
                SELECT id, idDocente, idGrupo, codigoMateria, nombreMateria, seccion, dia, horaInicio, horaFin, totalEstudiantes
                FROM dbo.uv_horario_docente
                WHERE idDocente = :idDocente
                ORDER BY dia, horaInicio, nombreMateria
                """, new MapSqlParameterSource("idDocente", idDocente), (rs, rowNum) -> new HorarioDocenteProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idDocente")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigoMateria")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreMateria")),
                JdbcBaselineValueMapper.toString(rs.getObject("seccion")),
                JdbcBaselineValueMapper.toString(rs.getObject("dia")),
                JdbcBaselineValueMapper.toLocalTime(rs.getObject("horaInicio")),
                JdbcBaselineValueMapper.toLocalTime(rs.getObject("horaFin")),
                JdbcBaselineValueMapper.toInteger(rs.getObject("totalEstudiantes"))
        ));
    }
}


