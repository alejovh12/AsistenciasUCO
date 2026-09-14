package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class DecanoSqlServerAdapter implements DecanoQueryPort, DecanoCommandPort {

    private static final String ID_CORRELACION = "idCorrelacion";
    private final NamedParameterJdbcOperations jdbcOperations;
    private final CanonicalStoredProcedureExecutor procedureExecutor;

    public DecanoSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "CanonicalStoredProcedureExecutor es obligatorio.");
    }

    @Override
    public List<DecanoProjection> consultarDecanos() {
        return jdbcOperations.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano
                FROM dbo.uv_decano
                ORDER BY nombreCompleto, id
                """, (rs, rowNum) -> new DecanoProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcValueMapper.toUuid(rs.getObject("idFacultad")),
                JdbcValueMapper.toString(rs.getObject("nombreFacultad")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivoDecano"))
        ));
    }

    @Override
    public void crearDecano(final CrearDecanoCommand command) {
        procedureExecutor.execute("crearDecano", """
                EXEC dbo.usp_crear_decano
                     @idDecano = :idDecano,
                     @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                     @numeroIdentificacion = :numeroIdentificacion,
                     @primerNombre = :primerNombre,
                     @segundoNombre = :segundoNombre,
                     @primerApellido = :primerApellido,
                     @segundoApellido = :segundoApellido,
                     @correo = :correo,
                     @idFacultad = :idFacultad,
                     @nombreFacultad = :nombreFacultad,
                     @password = :password,
                     @idCorrelacion = :idCorrelacion
                """, new MapSqlParameterSource()
                .addValue("idDecano", command.idDecano())
                .addValue("idTipoIdIdentificacion", command.tipoIdentificacionId())
                .addValue("numeroIdentificacion", command.numeroIdentificacion())
                .addValue("primerNombre", command.primerNombre())
                .addValue("segundoNombre", command.segundoNombre())
                .addValue("primerApellido", command.primerApellido())
                .addValue("segundoApellido", command.segundoApellido())
                .addValue("correo", command.correo())
                .addValue("password", command.password())
                .addValue("idFacultad", command.idFacultad())
                .addValue("nombreFacultad", command.nombreFacultad())
                .addValue(ID_CORRELACION, CorrelationIdContext.require()));
    }
}
