package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CoordinadorSqlServerAdapter implements CoordinadorQueryPort, CoordinadorCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CanonicalStoredProcedureExecutor procedureExecutor;

    public CoordinadorSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "CanonicalStoredProcedureExecutor es obligatorio.");
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
            final String password
    ) {
        procedureExecutor.execute("crearCoordinador", """
                EXEC dbo.usp_crear_coordinador
                     @idCoordinador = :idCoordinador,
                     @numeroIdentificacion = :numeroIdentificacion,
                     @primerNombre = :primerNombre,
                     @segundoNombre = :segundoNombre,
                     @primerApellido = :primerApellido,
                     @segundoApellido = :segundoApellido,
                     @correo = :correo,
                     @idPrograma = :idPrograma,
                     @idFacultad = :idFacultad,
                     @password = :password,
                     @idCorrelacion = :idCorrelacion
                """, new MapSqlParameterSource()
                .addValue("idCoordinador", idCoordinador)
                .addValue("numeroIdentificacion", numeroIdentificacion)
                .addValue("primerNombre", primerNombre)
                .addValue("segundoNombre", segundoNombre)
                .addValue("primerApellido", primerApellido)
                .addValue("segundoApellido", segundoApellido)
                .addValue("correo", correo)
                .addValue("idPrograma", idPrograma)
                .addValue("idFacultad", idFacultad)
                .addValue("password", password)
                .addValue("idCorrelacion", CorrelationIdContext.require()));
    }

    @Override
    public List<CoordinadorProjection> consultarCoordinadoresPorFacultad(final UUID idFacultad) {
        return jdbcOperations.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma, nombrePrograma, estaActivoCoordinador
                FROM dbo.uv_coordinador
                WHERE idFacultad = :idFacultad
                ORDER BY nombrePrograma, nombreCompleto, id
                """, new MapSqlParameterSource("idFacultad", idFacultad), (rs, rowNum) -> new CoordinadorProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivoCoordinador"))
        ));
    }
}
