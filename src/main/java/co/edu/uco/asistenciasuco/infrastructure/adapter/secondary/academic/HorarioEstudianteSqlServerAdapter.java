package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class HorarioEstudianteSqlServerAdapter implements HorarioEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public HorarioEstudianteSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<HorarioEstudianteProjection> consultarHorarioEstudiante(final UUID idEstudiante) {
        return jdbcOperations.query("""
                SELECT id, idEstudiante, idGrupo, codigoMateria, nombreMateria, grupo, dia, horaInicio, horaFin, aula, docente
                FROM dbo.uv_horario_estudiante
                WHERE idEstudiante = :idEstudiante
                ORDER BY dia, horaInicio, nombreMateria
                """, new MapSqlParameterSource("idEstudiante", idEstudiante), (rs, rowNum) -> new HorarioEstudianteProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idEstudiante")),
                JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcValueMapper.toString(rs.getObject("codigoMateria")),
                JdbcValueMapper.toString(rs.getObject("nombreMateria")),
                JdbcValueMapper.toString(rs.getObject("grupo")),
                JdbcValueMapper.toString(rs.getObject("dia")),
                JdbcValueMapper.toLocalTime(rs.getObject("horaInicio")),
                JdbcValueMapper.toLocalTime(rs.getObject("horaFin")),
                JdbcValueMapper.toString(rs.getObject("aula")),
                JdbcValueMapper.toString(rs.getObject("docente"))
        ));
    }
}
