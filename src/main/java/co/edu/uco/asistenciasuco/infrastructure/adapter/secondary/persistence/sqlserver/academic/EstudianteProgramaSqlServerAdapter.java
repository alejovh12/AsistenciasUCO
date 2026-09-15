package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EstudianteProgramaSqlServerAdapter implements EstudianteProgramaQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public EstudianteProgramaSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
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
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcValueMapper.toString(rs.getObject("correo")),
                JdbcValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcValueMapper.toString(rs.getObject("nombrePrograma"))
        ));
    }
}
