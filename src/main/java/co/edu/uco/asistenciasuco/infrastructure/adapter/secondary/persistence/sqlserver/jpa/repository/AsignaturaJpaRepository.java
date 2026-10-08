package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Repositorio JPA directo consolidado para Asignatura. */
@Repository
public class AsignaturaJpaRepository implements AsignaturaQueryPort, AsignaturaCommandPort {

    static final String SQL_CREAR_ASIGNATURA = """
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
            """;

    static final String SQL_ACTUALIZAR_ASIGNATURA = """
            EXEC dbo.usp_actualizar_asignatura
                 @idAsignatura = :idAsignatura,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @creditos = :creditos,
                 @idPlanEstudio = :idPlanEstudio,
                 @semestreNumero = :semestreNumero,
                 @nombreArea = :nombreArea,
                 @nombreComponente = :nombreComponente,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_TOGGLE_ESTADO_ASIGNATURA = """
            EXEC dbo.usp_toggle_estado_asignatura
                 @idAsignatura = :idAsignatura,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String HQL_SELECT = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow(
                a.id, a.codigo, a.nombre, a.credito, a.idArea, a.nombreArea, a.idComponente, a.nombreComponente,
                a.idSemestrePlanEstudio, sp.idPlanEstudio, sp.idPrograma, a.nombrePrograma, a.codigoSemestre,
                a.estaActivaAsignatura, a.estaActivaTextoAsignatura)
            from UvAsignaturaEntity a, UvSemestrePlanEstudioEntity sp
            where sp.id = a.idSemestrePlanEstudio
            """;
    static final String HQL_POR_PROGRAMA = HQL_SELECT
            + "  and sp.idPrograma = :idPrograma\norder by a.nombre, a.codigo, a.id";
    static final String HQL_POR_PLAN = HQL_SELECT
            + "  and sp.idPlanEstudio = :idPlanEstudio\norder by a.codigoSemestre, a.nombre, a.codigo, a.id";

    static final String OP_CREAR = "crearAsignatura";
    static final String OP_ACTUALIZAR = "actualizarAsignatura";
    static final String OP_TOGGLE = "toggleEstadoAsignatura";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AsignaturaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearAsignatura(final UUID idAsignatura, final String codigo, final String nombre,
                                final Integer creditos, final UUID idPlanEstudio, final Integer semestreNumero,
                                final String nombreArea, final String nombreComponente, final UUID usuarioEjecutor) {
        ejecutarCrearAsignatura(idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero,
                nombreArea, nombreComponente, usuarioEjecutor);
    }

    @Override
    public void actualizarAsignatura(final UUID idAsignatura, final String codigo, final String nombre,
                                     final Integer creditos, final UUID idPlanEstudio, final Integer semestreNumero,
                                     final String nombreArea, final String nombreComponente) {
        ejecutarActualizarAsignatura(idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero,
                nombreArea, nombreComponente);
    }

    @Override
    public void toggleEstadoAsignatura(final UUID idAsignatura) {
        ejecutarToggleEstadoAsignatura(idAsignatura);
    }

    // QUERIES

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPrograma(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarAsignaturasPorPrograma",
                "No fue posible consultar las asignaturas desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_PROGRAMA, AsignaturaQueryRow.class)
                        .setParameter("idPrograma", idPrograma)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toAsignatura)
                        .toList()
        );
    }

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPlan(final UUID idPlanEstudio) {
        return JpaQueryExecutor.execute(
                "consultarAsignaturasPorPlan",
                "No fue posible consultar las asignaturas del plan desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_PLAN, AsignaturaQueryRow.class)
                        .setParameter("idPlanEstudio", idPlanEstudio)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toAsignatura)
                        .toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarCrearAsignatura(
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
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idAsignatura", idAsignatura);
        parametros.put("codigo", codigo);
        parametros.put("nombre", nombre);
        parametros.put("creditos", creditos);
        parametros.put("idPlanEstudio", idPlanEstudio);
        parametros.put("semestreNumero", semestreNumero);
        parametros.put("nombreArea", nombreArea);
        parametros.put("nombreComponente", nombreComponente);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);

        return procedureExecutor.execute(OP_CREAR, SQL_CREAR_ASIGNATURA, parametros, correlationId);
    }

    private ProcedureResult ejecutarActualizarAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente
    ) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idAsignatura", idAsignatura);
        parametros.put("codigo", codigo);
        parametros.put("nombre", nombre);
        parametros.put("creditos", creditos);
        parametros.put("idPlanEstudio", idPlanEstudio);
        parametros.put("semestreNumero", semestreNumero);
        parametros.put("nombreArea", nombreArea);
        parametros.put("nombreComponente", nombreComponente);
        parametros.put("idCorrelacion", correlationId);

        return procedureExecutor.execute(OP_ACTUALIZAR, SQL_ACTUALIZAR_ASIGNATURA, parametros, correlationId);
    }

    private ProcedureResult ejecutarToggleEstadoAsignatura(final UUID idAsignatura) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idAsignatura", idAsignatura);
        parametros.put("idCorrelacion", correlationId);

        return procedureExecutor.execute(OP_TOGGLE, SQL_TOGGLE_ESTADO_ASIGNATURA, parametros, correlationId);
    }
}
