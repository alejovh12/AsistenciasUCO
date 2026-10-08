package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.ReporteAsistenciaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reporte de asistencia por grupo sobre JPA (LB-008 JPA-05). Reutiliza las entidades de vista existentes y
 * devuelve filas de constructor, nunca entidades JPA fuera de Infrastructure.
 * Las sesiones usan la conversion UTC certificada en JPA-04.
 */
@Repository
public class ReporteAsistenciaJpaRepository implements ReporteAsistenciaQueryPort {

    static final String HQL_REPORTE_GRUPO = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.ReporteAsistenciaQueryRow(
                s.codigoGrupo, s.nombreGrupo, s.numero, s.nombre, s.fechaHoraInicio, s.fechaHoraFin,
                ei.numeroIdentificacion, ei.nombreCompleto, u.correo, da.asistio, da.nombreRazonCausa)
            from UvSesionEntity s
            join UvEstudianteGrupoEntity eg on eg.idGrupo = s.idGrupo
            join UvEstudianteIdentidadEntity ei on ei.id = eg.idEstudiante
            join UvUsuarioEntity u on u.id = ei.idUsuario
            left join UvAsistenciaEntity a on a.idSesion = s.id and a.idEstudianteGrupo = eg.id
            left join UvDetalleAsistenciaEntity da on da.idAsistencia = a.id
            where s.idGrupo = :idGrupo
            order by s.numero, s.fechaHoraInicio, ei.nombreCompleto
            """;

    private final EntityManager entityManager;

    public ReporteAsistenciaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de reporte es obligatorio.");
    }

    @Override
    public List<ReporteAsistenciaRow> consultarReporteAsistenciaGrupo(final UUID grupoId) {
        return JpaQueryExecutor.execute(
                "consultarReporteAsistenciaGrupo",
                "No fue posible consultar el reporte de asistencia.",
                () -> entityManager.createQuery(HQL_REPORTE_GRUPO, ReporteAsistenciaQueryRow.class)
                        .setParameter("idGrupo", grupoId)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toReporteAsistencia)
                        .toList()
        );
    }
}

