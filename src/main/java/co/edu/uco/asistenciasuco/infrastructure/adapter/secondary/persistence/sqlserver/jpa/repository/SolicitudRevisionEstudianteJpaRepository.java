package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSolicitudRevisionEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Solicitudes de revision del estudiante autenticado (HU010/HU011) por
 * {@code usp_consultar_solicitudes_revision_estudiante}. La paginacion es real (OFFSET/FETCH en SQL) y el
 * ownership se filtra en la DB por {@code Estudiante.usuario}.
 */
@Repository
public class SolicitudRevisionEstudianteJpaRepository implements SolicitudRevisionEstudianteQueryPort {

    static final String OP_CONSULTAR = "consultarSolicitudesRevisionEstudiante";

    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_solicitudes_revision_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @codigoEstado = :codigoEstado,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    private final EntityManager entityManager;

    public SolicitudRevisionEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public List<SolicitudRevisionEstudianteProjection> consultarSolicitudesEstudiante(
            final UUID usuarioEjecutor,
            final String codigoEstado,
            final int numeroPagina,
            final int tamanoPagina
    ) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR,
                "No fue posible consultar las solicitudes de revision del estudiante desde base de datos.",
                () -> entityManager.createNativeQuery(SQL_CONSULTAR, SpSolicitudRevisionEstudianteEntity.class)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .setParameter("codigoEstado", codigoEstado)
                        .setParameter("numeroPagina", numeroPagina)
                        .setParameter("tamanoPagina", tamanoPagina)
                        .getResultList().stream()
                        .map(fila -> toProjection((SpSolicitudRevisionEstudianteEntity) fila))
                        .toList()
        );
    }

    private static SolicitudRevisionEstudianteProjection toProjection(final SpSolicitudRevisionEstudianteEntity fila) {
        final String procedencia = fila.procedenciaTemporalSesion();
        return new SolicitudRevisionEstudianteProjection(
                fila.idSolicitudRevisionAsistencia(),
                fila.nombreSolicitud(),
                fila.fechaSolicitud(),
                fila.codigoEstado(),
                fila.nombreEstado(),
                fila.justificacionSolicitud(),
                fila.justificacionRespuesta(),
                fila.idSesion(),
                fila.numeroSesion(),
                // Solo se afirma un instante UTC cuando la DB conoce la procedencia (contrato UTC v2).
                procedencia == null ? null : SoporteArchivoJpaRepository.toUtcInstant(fila.fechaHoraInicioSesion()),
                procedencia,
                fila.idGrupo(),
                fila.codigoGrupo(),
                fila.nombreGrupo(),
                fila.idAsignatura(),
                fila.codigoAsignatura(),
                fila.nombreAsignatura(),
                fila.idArchivoSoporte(),
                fila.nombreSoporte(),
                fila.tipoContenidoSoporte(),
                fila.tamanioSoporteBytes(),
                SoporteArchivoJpaRepository.toUtcInstant(fila.fechaRegistroSoporteUtc()),
                fila.totalRegistros() == null ? 0 : fila.totalRegistros()
        );
    }
}
