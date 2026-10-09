package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EstudianteProgramaJdbcBaseline implements EstudianteProgramaQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public EstudianteProgramaJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<EstudianteProgramaProjection> consultarEstudiantesPorPrograma(final UUID idPrograma) {
        return jdbcOperations.query("""
                SELECT ep.id, ei.idUsuario, ei.numeroIdentificacion, ei.nombreCompleto, u.correo,
                       ep.idPrograma, ep.nombrePrograma
                FROM dbo.uv_estudiante_programa ep
                INNER JOIN dbo.uv_estudiante_identidad ei ON ei.id = ep.idEstudiante
                INNER JOIN dbo.uv_usuario u ON u.id = ei.idUsuario
                WHERE ep.idPrograma = :idPrograma
                ORDER BY ei.nombreCompleto, ep.id
                """, new MapSqlParameterSource("idPrograma", idPrograma), (rs, rowNum) -> new EstudianteProgramaProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcBaselineValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcBaselineValueMapper.toString(rs.getObject("correo")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombrePrograma"))
        ));
    }
}


