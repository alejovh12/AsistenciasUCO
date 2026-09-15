package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PlanEstudioSqlServerAdapter implements PlanEstudioQueryPort, PlanEstudioCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CanonicalStoredProcedureExecutor procedureExecutor;

    public PlanEstudioSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "CanonicalStoredProcedureExecutor es obligatorio.");
    }

    @Override
    public void registrarOActualizarPlanEstudio(
            final UUID idPlanEstudio,
            final UUID idPrograma,
            final String codigo,
            final String nombre
    ) {
        procedureExecutor.execute("registrarOActualizarPlanEstudio", """
                EXEC dbo.usp_registrar_o_actualizar_plan_estudio
                     @idPlanEstudio = :idPlanEstudio,
                     @idPrograma = :idPrograma,
                     @codigo = :codigo,
                     @nombre = :nombre,
                     @idCorrelacion = :idCorrelacion
                """, new MapSqlParameterSource()
                .addValue("idPlanEstudio", idPlanEstudio)
                .addValue("idPrograma", idPrograma)
                .addValue("codigo", codigo)
                .addValue("nombre", nombre)
                .addValue("idCorrelacion", CorrelationIdContext.require()));
    }

    @Override
    public List<PlanEstudioProjection> consultarPlanesPorPrograma(final UUID idPrograma) {
        return jdbcOperations.query("""
                SELECT id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio,
                       estaActivoTextoPlanEstudio, justificacionEstado
                FROM dbo.uv_plan_estudio
                WHERE idPrograma = :idPrograma
                ORDER BY nombrePrograma, inp, id
                """, new MapSqlParameterSource("idPrograma", idPrograma), (rs, rowNum) -> new PlanEstudioProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcValueMapper.toString(rs.getObject("inp")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivoPlanEstudio")),
                JdbcValueMapper.toString(rs.getObject("estaActivoTextoPlanEstudio")),
                JdbcValueMapper.toString(rs.getObject("justificacionEstado"))
        ));
    }
}
