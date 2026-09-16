package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AsignaturaSqlServerAdapter implements AsignaturaQueryPort, AsignaturaCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CanonicalStoredProcedureExecutor procedureExecutor;

    public AsignaturaSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "CanonicalStoredProcedureExecutor es obligatorio.");
    }

    @Override
    public void crearAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente,
            final UUID usuarioEjecutor
    ) {
        procedureExecutor.execute("crearAsignatura", """
                EXEC dbo.usp_crear_asignatura
                     @idAsignatura = :idAsignatura,
                     @codigo = :codigo,
                     @nombre = :nombre,
                     @creditos = :creditos,
                     @idPlanEstudio = :idPlanEstudio,
                     @semestreNumero = :semestreNumero,
                     @nombreArea = :nombreArea,
                     @nombreComponente = :nombreComponente,
                     @idCorrelacion = :idCorrelacion,
                     @idUsuarioEjecutor = :idUsuarioEjecutor
                """, new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue("codigo", codigo)
                .addValue("nombre", nombre)
                .addValue("creditos", creditos)
                .addValue("idPlanEstudio", idPlanEstudio)
                .addValue("semestreNumero", semestreNumero)
                .addValue("nombreArea", nombreArea)
                .addValue("nombreComponente", nombreComponente)
                .addValue("idCorrelacion", CorrelationIdContext.require())
                .addValue("idUsuarioEjecutor", usuarioEjecutor));
    }

    @Override
    public void actualizarAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente
    ) {
        ejecutarGuardarAsignatura("actualizarAsignatura", "usp_actualizar_asignatura", idAsignatura, codigo, nombre, creditos,
                idPlanEstudio, semestreNumero, nombreArea, nombreComponente);
    }

    @Override
    public void toggleEstadoAsignatura(final UUID idAsignatura) {
        procedureExecutor.execute("toggleEstadoAsignatura", """
                EXEC dbo.usp_toggle_estado_asignatura
                     @idAsignatura = :idAsignatura,
                     @idCorrelacion = :idCorrelacion
                """, new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue("idCorrelacion", CorrelationIdContext.require()));
    }

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPrograma(final UUID idPrograma) {
        return jdbcOperations.query("""
                SELECT a.id, a.codigo, a.nombre, a.credito, a.idArea, a.nombreArea,
                       a.idComponente, a.nombreComponente, a.idSemestrePlanEstudio,
                       sp.idPlanEstudio, sp.idPrograma, a.nombrePrograma, a.codigoSemestre,
                       a.estaActivaAsignatura, a.estaActivaTextoAsignatura
                FROM dbo.uv_asignatura a
                INNER JOIN dbo.uv_semestre_plan_estudio sp ON sp.id = a.idSemestrePlanEstudio
                WHERE sp.idPrograma = :idPrograma
                ORDER BY nombre, codigo, id
                """, new MapSqlParameterSource("idPrograma", idPrograma), (rs, rowNum) -> mapAsignatura(rs));
    }

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPlan(final UUID idPlanEstudio) {
        return jdbcOperations.query("""
                SELECT a.id, a.codigo, a.nombre, a.credito, a.idArea, a.nombreArea,
                       a.idComponente, a.nombreComponente, a.idSemestrePlanEstudio,
                       sp.idPlanEstudio, sp.idPrograma, a.nombrePrograma, a.codigoSemestre,
                       a.estaActivaAsignatura, a.estaActivaTextoAsignatura
                FROM dbo.uv_asignatura a
                INNER JOIN dbo.uv_semestre_plan_estudio sp ON sp.id = a.idSemestrePlanEstudio
                WHERE sp.idPlanEstudio = :idPlanEstudio
                ORDER BY a.codigoSemestre, a.nombre, a.codigo, a.id
                """, new MapSqlParameterSource("idPlanEstudio", idPlanEstudio), (rs, rowNum) -> mapAsignatura(rs));
    }

    private void ejecutarGuardarAsignatura(
            final String operation,
            final String procedureName,
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente
    ) {
        procedureExecutor.execute(operation, """
                EXEC dbo.%s
                     @idAsignatura = :idAsignatura,
                     @codigo = :codigo,
                     @nombre = :nombre,
                     @creditos = :creditos,
                     @idPlanEstudio = :idPlanEstudio,
                     @semestreNumero = :semestreNumero,
                     @nombreArea = :nombreArea,
                     @nombreComponente = :nombreComponente,
                     @idCorrelacion = :idCorrelacion
                """.formatted(procedureName), new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue("codigo", codigo)
                .addValue("nombre", nombre)
                .addValue("creditos", creditos)
                .addValue("idPlanEstudio", idPlanEstudio)
                .addValue("semestreNumero", semestreNumero)
                .addValue("nombreArea", nombreArea)
                .addValue("nombreComponente", nombreComponente)
                .addValue("idCorrelacion", CorrelationIdContext.require()));
    }

    private static AsignaturaProjection mapAsignatura(final java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AsignaturaProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toString(rs.getObject("codigo")),
                JdbcValueMapper.toString(rs.getObject("nombre")),
                JdbcValueMapper.toInteger(rs.getObject("credito")),
                JdbcValueMapper.toUuid(rs.getObject("idArea")),
                JdbcValueMapper.toString(rs.getObject("nombreArea")),
                JdbcValueMapper.toUuid(rs.getObject("idComponente")),
                JdbcValueMapper.toString(rs.getObject("nombreComponente")),
                JdbcValueMapper.toUuid(rs.getObject("idSemestrePlanEstudio")),
                JdbcValueMapper.toUuid(rs.getObject("idPlanEstudio")),
                JdbcValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcValueMapper.toString(rs.getObject("codigoSemestre")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivaAsignatura")),
                JdbcValueMapper.toString(rs.getObject("estaActivaTextoAsignatura"))
        );
    }
}
