package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class HorarioDocenteSqlServerAdapter implements HorarioDocenteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public HorarioDocenteSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<HorarioDocenteProjection> consultarHorarioDocente(final UUID idDocente) {
        return jdbcOperations.query("""
                SELECT id, idDocente, idGrupo, codigoMateria, nombreMateria, seccion, dia, horaInicio, horaFin, aula, totalEstudiantes
                FROM dbo.uv_horario_docente
                WHERE idDocente = :idDocente
                ORDER BY dia, horaInicio, nombreMateria
                """, new MapSqlParameterSource("idDocente", idDocente), (rs, rowNum) -> new HorarioDocenteProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idDocente")),
                JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcValueMapper.toString(rs.getObject("codigoMateria")),
                JdbcValueMapper.toString(rs.getObject("nombreMateria")),
                JdbcValueMapper.toString(rs.getObject("seccion")),
                JdbcValueMapper.toString(rs.getObject("dia")),
                JdbcValueMapper.toLocalTime(rs.getObject("horaInicio")),
                JdbcValueMapper.toLocalTime(rs.getObject("horaFin")),
                JdbcValueMapper.toString(rs.getObject("aula")),
                JdbcValueMapper.toInteger(rs.getObject("totalEstudiantes"))
        ));
    }
}
