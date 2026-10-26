package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAsistenciaSesionPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpGrupoProgramaPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSesionGrupoPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

/**
 * HU064/HU060/HU061: native EXEC de los SP paginados de lectura. El SP filtra por el usuario ejecutor (vistas
 * {@code uv_auth_*}), por lo que el aislamiento por programa queda en el provider DB.
 */
@Repository
public class AsistenciaProgramaJpaRepository implements AsistenciaProgramaQueryPort {

    static final String SQL_GRUPOS = """
            EXEC dbo.usp_consultar_grupos_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @idAsignatura = NULL,
                 @filtroTexto = :filtroTexto,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_SESIONES = """
            EXEC dbo.usp_consultar_sesiones_grupo_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idGrupo = :idGrupo,
                 @fechaDesde = :fechaDesde,
                 @fechaHasta = :fechaHasta,
                 @numeroSemana = NULL,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_ASISTENCIAS = """
            EXEC dbo.usp_consultar_asistencias_sesion_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idSesion = :idSesion,
                 @idRazonCausa = NULL,
                 @asistio = :asistio,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final String MENSAJE_PAGINACION_INVALIDA = "Los parametros de paginacion no son validos.";

    private final EntityManager entityManager;

    public AsistenciaProgramaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public Pagina<GrupoProgramaProjection> consultarGrupos(final UUID idUsuarioEjecutor, final UUID idPeriodoAcademico,
                                                            final String busqueda, final int pagina, final int tamano) {
        return consultar("consultarGruposPrograma", "No fue posible consultar los grupos del programa.",
                SQL_GRUPOS, SpGrupoProgramaPaginadoEntity.class, idUsuarioEjecutor, pagina, tamano,
                q -> q.setParameter("idPeriodoAcademico", idPeriodoAcademico).setParameter("filtroTexto", busqueda),
                SpGrupoProgramaPaginadoEntity::totalRegistros,
                e -> new GrupoProgramaProjection(e.id(), e.codigo(), e.nombre(), e.idAsignatura(), e.nombreAsignatura(),
                        e.idDocente(), e.capacidadMaximaPermitida(), e.estudiantesActivos(), e.cuposDisponibles(),
                        Boolean.TRUE.equals(e.grupoEstaHablitado()), e.fechaInicioPeriodoAcademico(),
                        e.fechaFinPeriodoAcademico()));
    }

    @Override
    public Pagina<SesionGrupoProjection> consultarSesionesGrupo(final UUID idUsuarioEjecutor, final UUID idGrupo,
                                                                 final LocalDate desde, final LocalDate hasta,
                                                                 final int pagina, final int tamano) {
        return consultar("consultarSesionesGrupo", "No fue posible consultar las sesiones del grupo.",
                SQL_SESIONES, SpSesionGrupoPaginadoEntity.class, idUsuarioEjecutor, pagina, tamano,
                q -> q.setParameter("idGrupo", idGrupo).setParameter("fechaDesde", desde)
                        .setParameter("fechaHasta", hasta),
                SpSesionGrupoPaginadoEntity::totalRegistros,
                e -> new SesionGrupoProjection(e.id(), e.idGrupo(), e.codigoGrupo(), e.nombreGrupo(), e.nombre(),
                        e.numero(), e.codigo(), e.numeroSemana(), e.fechaHoraInicio(), e.fechaHoraFin()));
    }

    @Override
    public Pagina<AsistenciaSesionProjection> consultarAsistenciasSesion(final UUID idUsuarioEjecutor,
                                                                          final UUID idSesion, final Boolean asistio,
                                                                          final int pagina, final int tamano) {
        return consultar("consultarAsistenciasSesion", "No fue posible consultar las asistencias de la sesion.",
                SQL_ASISTENCIAS, SpAsistenciaSesionPaginadoEntity.class, idUsuarioEjecutor, pagina, tamano,
                q -> q.setParameter("idSesion", idSesion).setParameter("asistio", asistio),
                SpAsistenciaSesionPaginadoEntity::totalRegistros,
                e -> new AsistenciaSesionProjection(e.idDetalleAsistencia(), e.idSesion(), e.nombreSesion(),
                        e.idEstudiante(),
                        e.numeroIdentificacion() == null ? null : String.valueOf(e.numeroIdentificacion()),
                        e.nombreCompleto(), Boolean.TRUE.equals(e.asistio()), e.codigoRazonCausa(),
                        e.nombreRazonCausa(), e.fechaHoraInicio(), e.fechaHoraFin()));
    }

    private <E, P> Pagina<P> consultar(final String operacion, final String mensajeFallo, final String sql,
                                       final Class<E> tipo, final UUID idUsuarioEjecutor, final int pagina,
                                       final int tamano, final UnaryOperator<Query> filtros,
                                       final Function<E, Integer> total, final Function<E, P> mapper) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(operacion, mensajeFallo, () -> {
            final List<?> filas;
            try {
                filas = filtros.apply(entityManager.createNativeQuery(sql, tipo)
                                .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                                .setParameter("idCorrelacion", correlationId)
                                .setParameter("numeroPagina", pagina)
                                .setParameter("tamanoPagina", tamano))
                        .getResultList();
            } catch (final PersistenceException exception) {
                if (esPaginacionInvalida(exception)) {
                    throw new ValidationException(MENSAJE_PAGINACION_INVALIDA);
                }
                throw exception;
            }
            final List<E> entidades = filas.stream().map(tipo::cast).toList();
            final ToIntFunction<E> totalDe = e -> total.apply(e) == null ? 0 : total.apply(e);
            final int totalRegistros = entidades.isEmpty() ? 0 : totalDe.applyAsInt(entidades.getFirst());
            return new Pagina<>(entidades.stream().map(mapper).toList(), totalRegistros);
        });
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
