package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class HorarioEstudianteJdbcBaseline implements HorarioEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public HorarioEstudianteJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<HorarioEstudianteProjection> consultarHorarioEstudiante(final UUID idEstudiante) {
        return jdbcOperations.query("""
                SELECT id, idEstudiante, idGrupo, codigoMateria, nombreMateria, grupo, dia, horaInicio, horaFin, docente
                FROM dbo.uv_horario_estudiante
                WHERE idEstudiante = :idEstudiante
                ORDER BY dia, horaInicio, nombreMateria
                """, new MapSqlParameterSource("idEstudiante", idEstudiante), (rs, rowNum) -> new HorarioEstudianteProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idEstudiante")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigoMateria")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreMateria")),
                JdbcBaselineValueMapper.toString(rs.getObject("grupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("dia")),
                JdbcBaselineValueMapper.toLocalTime(rs.getObject("horaInicio")),
                JdbcBaselineValueMapper.toLocalTime(rs.getObject("horaFin")),
                JdbcBaselineValueMapper.toString(rs.getObject("docente"))
        ));
    }
}


