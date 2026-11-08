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
    static final String SQL_MALLA = """
            EXEC dbo.usp_consultar_asignaturas_plan_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idPlanEstudio = :idPlanEstudio,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina,
                 @numeroSemestre = :numeroSemestre, @codigoCaracter = :codigoCaracter, @busqueda = :busqueda
            """;
    static final String SQL_ASIGNATURAS_COORDINADOR = """
            EXEC dbo.usp_consultar_asignaturas_coordinador_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina,
                 @idPrograma = :idPrograma, @busqueda = :busqueda, @estaActiva = :estaActiva
            """;
    static final String SQL_PLANES = """
            EXEC dbo.usp_consultar_planes_estudio_programa_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina,
                 @idPrograma = :idPrograma, @estaActivo = :estaActivo
            """;
    static final String SQL_GRUPOS_FACULTAD = """
            EXEC dbo.usp_consultar_grupos_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina,
                 @idPeriodoAcademico = :idPeriodoAcademico, @idPrograma = :idPrograma, @busqueda = :busqueda
            """;
    static final String SQL_GRUPO_FACULTAD = """
            EXEC dbo.usp_consultar_grupo_facultad_detalle
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idGrupo = :idGrupo
            """;
    static final int COLUMNAS_GRUPO_DETALLE = 12;
    static final int COLUMNAS_ASIGNATURA_PLAN = 21;
    static final int COLUMNAS_PLAN = 11;
    static final int COLUMNAS_GRUPO_FACULTAD = 13;
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

    @Override
    public PaginaAsignaturasPlanProjection consultarMalla(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final Integer numeroSemestre,
                                                          final String codigoCaracter, final String busqueda, final int pagina, final int tamano) {
        final Map<String, Object> p = paginado(idUsuarioEjecutor, pagina, tamano);
        p.put("idPlanEstudio", idPlanEstudio);
        p.put("numeroSemestre", numeroSemestre);
        p.put("codigoCaracter", codigoCaracter);
        p.put("busqueda", busqueda);
        return JpaQueryExecutor.execute("consultarMallaPlan", "No fue posible consultar la malla del plan.", () -> asignaturasPlan(SQL_MALLA, p));
    }

    @Override
    public PaginaAsignaturasPlanProjection consultarAsignaturasCoordinador(final UUID idUsuarioEjecutor, final UUID idPrograma, final String busqueda,
                                                                           final Boolean estaActiva, final int pagina, final int tamano) {
        final Map<String, Object> p = paginado(idUsuarioEjecutor, pagina, tamano);
        p.put("idPrograma", idPrograma);
        p.put("busqueda", busqueda);
        p.put("estaActiva", estaActiva);
        return JpaQueryExecutor.execute("consultarAsignaturasCoordinador", "No fue posible consultar las asignaturas del coordinador.", () -> asignaturasPlan(SQL_ASIGNATURAS_COORDINADOR, p));
    }

    @Override
    public PaginaPlanesEstudioProjection consultarPlanes(final UUID idUsuarioEjecutor, final UUID idPrograma, final Boolean estaActivo, final int pagina, final int tamano) {
        final Map<String, Object> p = paginado(idUsuarioEjecutor, pagina, tamano);
        p.put("idPrograma", idPrograma);
        p.put("estaActivo", estaActivo);
        return JpaQueryExecutor.execute("consultarPlanesEstudioPrograma", "No fue posible consultar los planes de estudio.", () -> {
            long total = 0;
            final List<PlanEstudioProjection> lista = new java.util.ArrayList<>();
            for (final Object fila : consulta(SQL_PLANES, p)) {
                final Object[] c = exigirColumnas((Object[]) fila, COLUMNAS_PLAN, "usp_consultar_planes_estudio_programa_paginado");
                lista.add(new PlanEstudioProjection(uuid(c[0]), uuid(c[1]), (String) c[2], c[3] == null ? null : c[3].toString(), bool(c[4]), (String) c[5], (String) c[6],
                        entero(c[7]), entero(c[8]), entero(c[9])));
                total = ((Number) c[10]).longValue();
            }
            return new PaginaPlanesEstudioProjection(lista, total);
        });
    }

    @Override
    public PaginaGruposFacultadProjection consultarGruposFacultad(final UUID idUsuarioEjecutor, final UUID idPeriodoAcademico, final UUID idPrograma,
                                                                  final String busqueda, final int pagina, final int tamano) {
        final Map<String, Object> p = paginado(idUsuarioEjecutor, pagina, tamano);
        p.put("idPeriodoAcademico", idPeriodoAcademico);
        p.put("idPrograma", idPrograma);
        p.put("busqueda", busqueda);
        return JpaQueryExecutor.execute("consultarGruposFacultad", "No fue posible consultar los grupos de la facultad.", () -> {
            long total = 0;
            final List<GrupoFacultadProjection> lista = new java.util.ArrayList<>();
            for (final Object fila : consulta(SQL_GRUPOS_FACULTAD, p)) {
                final Object[] c = exigirColumnas((Object[]) fila, COLUMNAS_GRUPO_FACULTAD, "usp_consultar_grupos_facultad_paginado");
                lista.add(new GrupoFacultadProjection(uuid(c[0]), entero(c[1]), (String) c[2], uuid(c[3]), uuid(c[4]), (String) c[5], uuid(c[6]), entero(c[7]),
                        entero(c[8]), entero(c[9]), bool(c[10]), nombres((String) c[11])));
                total = ((Number) c[12]).longValue();
            }
            return new PaginaGruposFacultadProjection(lista, total);
        });
    }

    @Override
    public GrupoFacultadProjection consultarGrupoFacultad(final UUID idUsuarioEjecutor, final UUID idGrupo) {
        final Map<String, Object> p = new LinkedHashMap<>();
        p.put("idUsuarioEjecutor", idUsuarioEjecutor);
        p.put("idCorrelacion", CorrelationIdContext.require());
        p.put("idGrupo", idGrupo);
        return JpaQueryExecutor.execute("consultarGrupoFacultad", "No fue posible consultar el grupo de la facultad.", () -> {
            final List<?> filas = consulta(SQL_GRUPO_FACULTAD, p);
            if (filas.isEmpty()) {
                // El SP ya lanzo SEC_002 para un grupo inexistente o ajeno; sin fila nunca se devuelve un grupo vacio.
                throw new IllegalStateException("usp_consultar_grupo_facultad_detalle no devolvio el grupo solicitado.");
            }
            final Object[] c = exigirColumnas((Object[]) filas.getFirst(), COLUMNAS_GRUPO_DETALLE, "usp_consultar_grupo_facultad_detalle");
            return new GrupoFacultadProjection(uuid(c[0]), entero(c[1]), (String) c[2], uuid(c[3]), uuid(c[4]), (String) c[5], uuid(c[6]), entero(c[7]),
                    entero(c[8]), entero(c[9]), bool(c[10]), nombres((String) c[11]));
        });
    }

    private PaginaAsignaturasPlanProjection asignaturasPlan(final String sql, final Map<String, Object> parametros) {
        long total = 0;
        final List<AsignaturaPlanProjection> lista = new java.util.ArrayList<>();
        for (final Object fila : consulta(sql, parametros)) {
            final Object[] c = exigirColumnas((Object[]) fila, COLUMNAS_ASIGNATURA_PLAN, "consulta de asignaturas por plan");
            lista.add(new AsignaturaPlanProjection(uuid(c[0]), (String) c[1], (String) c[2], entero(c[3]), uuid(c[4]), (String) c[5], uuid(c[6]), (String) c[7],
                    uuid(c[8]), entero(c[9]), uuid(c[10]), (String) c[11], uuid(c[12]), entero(c[13]), (String) c[14], (String) c[15], (String) c[16],
                    bool(c[17]), bool(c[18]), bool(c[19])));
            total = ((Number) c[20]).longValue();
        }
        return new PaginaAsignaturasPlanProjection(lista, total);
    }

    private static Map<String, Object> paginado(final UUID idUsuarioEjecutor, final int pagina, final int tamano) {
        final Map<String, Object> p = new LinkedHashMap<>();
        p.put("idUsuarioEjecutor", idUsuarioEjecutor);
        p.put("idCorrelacion", CorrelationIdContext.require());
        p.put("numeroPagina", pagina);
        p.put("tamanoPagina", tamano);
        return p;
    }

    private List<?> consulta(final String sql, final Map<String, Object> parametros) {
        try {
            final var query = entityManager.createNativeQuery(sql);
            parametros.forEach(query::setParameter);
            return query.getResultList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception);
        }
    }

    private static Object[] exigirColumnas(final Object[] fila, final int esperadas, final String origen) {
        if (fila.length != esperadas) {
            throw new IllegalStateException(origen + " debe devolver " + esperadas + " columnas y devolvio " + fila.length + ".");
        }
        return fila;
    }

    private static boolean bool(final Object valor) {
        return valor instanceof Boolean b ? b : valor != null && Integer.parseInt(valor.toString()) != 0;
    }

    private static List<String> nombres(final String texto) {
        return texto == null || texto.isBlank() ? List.of()
                : java.util.Arrays.stream(texto.split(",")).map(String::strip).filter(x -> !x.isEmpty()).toList();
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
