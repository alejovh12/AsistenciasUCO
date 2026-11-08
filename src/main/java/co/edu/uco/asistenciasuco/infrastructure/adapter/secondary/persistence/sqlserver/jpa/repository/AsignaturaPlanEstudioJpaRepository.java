package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaPlanPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador JPA de asignaturas por plan sobre los SP de CULM-001 B2NM (lectura por posicion de columna del SP). */
@Repository
public class AsignaturaPlanEstudioJpaRepository implements AsignaturaPlanPort {

    static final String SQL_ASOCIAR = """
            EXEC dbo.usp_asociar_asignatura_plan
                 @idAsignatura = :idAsignatura,
                 @idPlanEstudio = :idPlanEstudio,
                 @semestreNumero = :semestreNumero,
                 @codigoCaracter = :codigoCaracter,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESVINCULAR = """
            EXEC dbo.usp_desvincular_asignatura_plan
                 @idAsignatura = :idAsignatura,
                 @idPlanEstudio = :idPlanEstudio,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ASOCIABLES = """
            EXEC dbo.usp_consultar_asignaturas_asociables_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPlanEstudio = :idPlanEstudio,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda
            """;
    static final String SQL_PROGRAMAS_GRUPO = """
            EXEC dbo.usp_consultar_programas_grupo
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idGrupo = :idGrupo
            """;
    static final String SQL_CARACTERES = "select codigo, nombre, orden from dbo.uv_caracter_asignatura_plan where estaActivo = 1 order by orden";
    static final String OP_ASOCIAR = "asociarAsignaturaPlan";
    static final String OP_DESVINCULAR = "desvincularAsignaturaPlan";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AsignaturaPlanEstudioJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public void asociar(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final UUID idAsignatura,
                        final Integer semestreNumero, final String codigoCaracter) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> p = new LinkedHashMap<>();
        p.put("idAsignatura", idAsignatura);
        p.put("idPlanEstudio", idPlanEstudio);
        p.put("semestreNumero", semestreNumero);
        p.put("codigoCaracter", codigoCaracter);
        p.put("idCorrelacion", correlationId);
        p.put("idUsuarioEjecutor", idUsuarioEjecutor);
        procedureExecutor.execute(OP_ASOCIAR, SQL_ASOCIAR, p, correlationId);
    }

    @Override
    public void desvincular(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final UUID idAsignatura) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> p = new LinkedHashMap<>();
        p.put("idAsignatura", idAsignatura);
        p.put("idPlanEstudio", idPlanEstudio);
        p.put("idCorrelacion", correlationId);
        p.put("idUsuarioEjecutor", idUsuarioEjecutor);
        procedureExecutor.execute(OP_DESVINCULAR, SQL_DESVINCULAR, p, correlationId);
    }

    @Override
    public PaginaAsignaturasAsociablesProjection consultarAsociables(final UUID idUsuarioEjecutor, final UUID idPlanEstudio,
                                                                   final String busqueda, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAsignaturasAsociables", "No fue posible consultar las asignaturas asociables.", () -> {
            final List<?> filas = consulta(SQL_ASOCIABLES, Map.of("idUsuarioEjecutor", idUsuarioEjecutor, "idCorrelacion", correlationId,
                    "idPlanEstudio", idPlanEstudio, "numeroPagina", pagina, "tamanoPagina", tamano), busqueda);
            long total = 0;
            final List<AsignaturaAsociableProjection> lista = new java.util.ArrayList<>();
            for (final Object fila : filas) {
                final Object[] c = (Object[]) fila;
                lista.add(new AsignaturaAsociableProjection(uuid(c[0]), (String) c[1], (String) c[2], entero(c[3]), (String) c[4], (String) c[5], (String) c[6]));
                total = ((Number) c[7]).longValue();
            }
            return new PaginaAsignaturasAsociablesProjection(lista, total);
        });
    }

    @Override
    public List<CaracterAsignaturaProjection> consultarCaracteres() {
        return JpaQueryExecutor.execute("consultarCaracteresAsignatura", "No fue posible consultar los caracteres de asignatura.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CARACTERES).getResultList()).stream()
                        .map(f -> (Object[]) f).map(c -> new CaracterAsignaturaProjection((String) c[0], (String) c[1], entero(c[2]))).toList());
    }

    @Override
    public List<ProgramaGrupoProjection> consultarProgramasGrupo(final UUID idUsuarioEjecutor, final UUID idGrupo) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarProgramasGrupo", "No fue posible consultar los programas del grupo.", () -> {
            final List<?> filas = consulta(SQL_PROGRAMAS_GRUPO, Map.of("idUsuarioEjecutor", idUsuarioEjecutor, "idCorrelacion", correlationId, "idGrupo", idGrupo), null);
            return filas.stream().map(f -> (Object[]) f).map(c -> new ProgramaGrupoProjection(uuid(c[0]), (String) c[1], uuid(c[2]), entero(c[3]))).toList();
        });
    }

    private List<?> consulta(final String sql, final Map<String, Object> parametros, final String busqueda) {
        try {
            final var query = entityManager.createNativeQuery(sql);
            parametros.forEach(query::setParameter);
            if (sql.contains(":busqueda")) {
                query.setParameter("busqueda", busqueda);
            }
            return query.getResultList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception);
        }
    }

    private static UUID uuid(final Object valor) {
        return valor == null ? null : (valor instanceof UUID u ? u : UUID.fromString(valor.toString()));
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }
}
