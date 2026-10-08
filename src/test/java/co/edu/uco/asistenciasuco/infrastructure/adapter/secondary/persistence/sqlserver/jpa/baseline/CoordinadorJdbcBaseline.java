package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CoordinadorJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server de coordinadores.
 *
 * <p>COMMAND (alta de coordinador) delega en {@link CoordinadorJpaRepository} sobre {@code EntityManager}
 * (LB-008 JPA-03). La QUERY sigue en JDBC temporal hasta JPA-05.</p>
 */
public final class CoordinadorJdbcBaseline implements CoordinadorQueryPort, CoordinadorCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CoordinadorJpaRepository commands;

    public CoordinadorJdbcBaseline(
            final NamedParameterJdbcOperations jdbcOperations,
            final CoordinadorJpaRepository commands
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.commands = Objects.requireNonNull(commands, "El command JPA de coordinador es obligatorio.");
    }

    @Override
    public void crearCoordinador(
            final UUID idCoordinador,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final UUID idFacultad,
            final String password,
            final UUID usuarioEjecutor
    ) {
        commands.crearCoordinador(idCoordinador, numeroIdentificacion, primerNombre, segundoNombre, primerApellido,
                segundoApellido, correo, idPrograma, idFacultad, password, usuarioEjecutor);
    }

    @Override
    public List<CoordinadorProjection> consultarCoordinadoresPorFacultad(final UUID idFacultad) {
        return jdbcOperations.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma, nombrePrograma, estaActivoCoordinador
                FROM dbo.uv_coordinador
                WHERE idFacultad = :idFacultad
                ORDER BY nombrePrograma, nombreCompleto, id
                """, new MapSqlParameterSource("idFacultad", idFacultad), (rs, rowNum) -> new CoordinadorProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcBaselineValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivoCoordinador"))
        ));
    }
}



