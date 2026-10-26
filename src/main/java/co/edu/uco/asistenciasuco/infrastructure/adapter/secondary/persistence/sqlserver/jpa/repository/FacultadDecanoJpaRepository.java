package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpEstructuraFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpEstudianteFacultadPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpMatriculaEstudianteFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSesionFacultadPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

/**
 * HU104/HU105/HU109/HU119/HU145: native EXEC de los SP de lectura del decano. El SP filtra por el decano ejecutor
 * (vistas {@code uv_auth_*}), por lo que el aislamiento por facultad queda en el provider DB.
 */
@Repository
public class FacultadDecanoJpaRepository implements FacultadDecanoQueryPort {

    static final String SQL_ESTUDIANTES = """
            EXEC dbo.usp_consultar_estudiantes_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @idPrograma = :idPrograma,
                 @estaActivo = :estaActivo
            """;
    static final String SQL_DETALLE = """
            EXEC dbo.usp_consultar_estudiante_facultad_detalle
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante
            """;
    static final String SQL_ESTRUCTURA = """
            EXEC dbo.usp_consultar_estructura_facultad
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_SESIONES = """
            EXEC dbo.usp_consultar_sesiones_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @fechaDesde = :fechaDesde,
                 @fechaHasta = :fechaHasta,
                 @idPrograma = :idPrograma
            """;
    static final String MENSAJE_PAGINACION_INVALIDA = "Los parametros de paginacion no son validos.";

    private final EntityManager entityManager;

    public FacultadDecanoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public Pagina<EstudianteFacultadProjection> consultarEstudiantes(final UUID idUsuarioEjecutor, final String busqueda,
                                                                     final UUID idPrograma, final Boolean estaActivo,
                                                                     final int pagina, final int tamano) {
        final List<SpEstudianteFacultadPaginadoEntity> filas = ejecutar("consultarEstudiantesFacultad",
                "No fue posible consultar los estudiantes de la facultad.", SQL_ESTUDIANTES,
                SpEstudianteFacultadPaginadoEntity.class, idUsuarioEjecutor,
                q -> q.setParameter("numeroPagina", pagina).setParameter("tamanoPagina", tamano)
                        .setParameter("busqueda", busqueda).setParameter("idPrograma", idPrograma)
                        .setParameter("estaActivo", estaActivo));
        return new Pagina<>(filas.stream().map(e -> new EstudianteFacultadProjection(e.idEstudiante(),
                texto(e.numeroIdentificacion()), e.nombreCompleto(), e.correo(),
                Boolean.TRUE.equals(e.estaActivoUsuario()), e.programas(), e.totalGrupos())).toList(),
                total(filas.isEmpty() ? null : filas.getFirst().totalRegistros()));
    }

    @Override
    public List<MatriculaEstudianteProjection> consultarDetalleEstudiante(final UUID idUsuarioEjecutor,
                                                                          final UUID idEstudiante) {
        return ejecutar("consultarDetalleEstudianteFacultad", "No fue posible consultar la ficha del estudiante.",
                SQL_DETALLE, SpMatriculaEstudianteFacultadEntity.class, idUsuarioEjecutor,
                q -> q.setParameter("idEstudiante", idEstudiante))
                .stream().map(e -> new MatriculaEstudianteProjection(e.idEstudiante(), texto(e.numeroIdentificacion()),
                        e.nombreCompleto(), e.idPrograma(), e.nombrePrograma(), e.inpPlanEstudio(), e.idAsignatura(),
                        e.nombreAsignatura(), e.idGrupo(), e.nombreGrupo(),
                        Boolean.TRUE.equals(e.estaActivoEstudiante())))
                .toList();
    }

    @Override
    public List<EstructuraFacultadProjection> consultarEstructura(final UUID idUsuarioEjecutor) {
        return ejecutar("consultarEstructuraFacultad", "No fue posible consultar la estructura de la facultad.",
                SQL_ESTRUCTURA, SpEstructuraFacultadEntity.class, idUsuarioEjecutor, UnaryOperator.identity())
                .stream().map(e -> new EstructuraFacultadProjection(e.idFacultad(), e.nombreFacultad(),
                        e.nombreCompletoDecano(), Boolean.TRUE.equals(e.estaActivaFacultad()), e.idPrograma(),
                        e.nombrePrograma(), e.nombreCoordinador(), Boolean.TRUE.equals(e.estaActivoPrograma()),
                        e.nombreTipoPrograma(), e.nombreModalidad(), e.nombreJornada(), e.idPlanEstudio(),
                        e.inpPlanEstudio(), Boolean.TRUE.equals(e.estaActivoPlanEstudio()), e.totalSemestres()))
                .toList();
    }

    @Override
    public Pagina<SesionFacultadProjection> consultarSesiones(final UUID idUsuarioEjecutor, final LocalDate desde,
                                                              final LocalDate hasta, final UUID idPrograma,
                                                              final int pagina, final int tamano) {
        final List<SpSesionFacultadPaginadoEntity> filas = ejecutar("consultarSesionesFacultad",
                "No fue posible consultar el cronograma de sesiones de la facultad.", SQL_SESIONES,
                SpSesionFacultadPaginadoEntity.class, idUsuarioEjecutor,
                q -> q.setParameter("numeroPagina", pagina).setParameter("tamanoPagina", tamano)
                        .setParameter("fechaDesde", desde).setParameter("fechaHasta", hasta)
                        .setParameter("idPrograma", idPrograma));
        return new Pagina<>(filas.stream().map(e -> new SesionFacultadProjection(e.id(), e.nombre(), e.codigo(),
                e.numeroSemana(), e.fechaHoraInicio(), e.fechaHoraFin(), e.idGrupo(), e.codigoGrupo(),
                e.nombreGrupo(), e.idAsignatura(), e.nombreAsignatura(), e.idPrograma(), e.nombrePrograma()))
                .toList(), total(filas.isEmpty() ? null : filas.getFirst().totalRegistros()));
    }

    private <E> List<E> ejecutar(final String operacion, final String mensajeFallo, final String sql,
                                 final Class<E> tipo, final UUID idUsuarioEjecutor,
                                 final UnaryOperator<Query> parametros) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(operacion, mensajeFallo, () -> {
            final List<?> filas;
            try {
                filas = parametros.apply(entityManager.createNativeQuery(sql, tipo)
                                .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                                .setParameter("idCorrelacion", correlationId))
                        .getResultList();
            } catch (final PersistenceException exception) {
                if (esPaginacionInvalida(exception)) {
                    throw new ValidationException(MENSAJE_PAGINACION_INVALIDA);
                }
                throw exception;
            }
            return filas.stream().map(tipo::cast).toList();
        });
    }

    private static int total(final Integer totalRegistros) {
        return totalRegistros == null ? 0 : totalRegistros;
    }

    private static String texto(final Integer numero) {
        return numero == null ? null : String.valueOf(numero);
    }

    /** El SP rechaza la paginacion fuera de rango con DBCODE=PAG_001: es una entrada invalida (400). */
    private static boolean esPaginacionInvalida(final Throwable exception) {
        for (Throwable actual = exception; actual != null; actual = actual.getCause()) {
            if (actual.getMessage() != null && actual.getMessage().contains("DBCODE=PAG_001|")) {
                return true;
            }
        }
        return false;
    }
}
