package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaSesionEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta JPA-only (solo lectura) de la asistencia propia del estudiante en las sesiones de una asignatura.
 * El filtro por {@code eg.idEstudiante} parte siempre del estudiante resuelto en el servidor, por lo que solo se
 * leen cabeceras de asistencia, detalles y solicitudes que pertenecen a la matricula de ese estudiante.
 * Left joins: una sesion con cabecera y sin detalle devuelve una fila con detalle nulo; una sesion sin cabecera
 * no devuelve fila (el caso de uso la clasifica como no disponible).
 */
@Repository
public class AsistenciaSesionEstudianteJpaRepository implements AsistenciaSesionEstudianteQueryPort {

    static final String HQL_POR_ESTUDIANTE_Y_ASIGNATURA = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaSesionEstudianteQueryRow(
                a.idSesion, da.codigo, da.asistio, da.codigoRazonCausa, sr.id, sr.nombreEstado, sr.fecha)
            from UvAsistenciaEntity a
            join UvEstudianteGrupoEntity eg on eg.id = a.idEstudianteGrupo
            join UvGrupoEntity g on g.id = eg.idGrupo
            left join UvDetalleAsistenciaEntity da on da.idAsistencia = a.id
            left join UvSolicitudRevisionAsistenciaEntity sr on sr.idAsistencia = a.id
            where eg.idEstudiante = :idEstudiante
              and g.idAsignatura = :idAsignatura
            """;

    /** Misma proyeccion y mismo aislamiento por estudiante/asignatura, acotada a las sesiones de la pagina. */
    static final String HQL_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES = HQL_POR_ESTUDIANTE_Y_ASIGNATURA
            + "  and a.idSesion in :idsSesion\n";

    /** Tope de parametros por sentencia (SQL Server admite 2100); una pagina normal cabe en un solo bloque. */
    static final int TAMANO_BLOQUE_IN = 1000;

    private final EntityManager entityManager;

    public AsistenciaSesionEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager,
                "El EntityManager de asistencia por sesion del estudiante es obligatorio.");
    }

    @Override
    public List<AsistenciaSesionEstudianteProjection> consultarAsistenciaSesiones(final UUID idEstudiante,
                                                                                  final UUID idAsignatura) {
        return JpaQueryExecutor.execute(
                "consultarAsistenciaSesionesEstudiante",
                "No fue posible consultar la asistencia de las sesiones desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ESTUDIANTE_Y_ASIGNATURA, AsistenciaSesionEstudianteQueryRow.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .setParameter("idAsignatura", idAsignatura)
                        .getResultList().stream()
                        .map(row -> new AsistenciaSesionEstudianteProjection(row.idSesion(), row.codigoDetalle(),
                                row.asistio(), row.codigoRazonCausa(), row.idSolicitud(), row.estadoSolicitud(),
                                row.fechaSolicitud()))
                        .toList()
        );
    }

    @Override
    public List<AsistenciaSesionEstudianteProjection> consultarAsistenciaSesiones(final UUID idEstudiante,
                                                                                  final UUID idAsignatura,
                                                                                  final Collection<UUID> idsSesion) {
        if (idsSesion == null || idsSesion.isEmpty()) {
            return List.of();
        }
        final List<UUID> ids = idsSesion.stream().filter(Objects::nonNull).distinct().toList();
        return JpaQueryExecutor.execute(
                "consultarAsistenciaSesionesEstudianteAcotada",
                "No fue posible consultar la asistencia de las sesiones desde base de datos.",
                () -> {
                    final List<AsistenciaSesionEstudianteProjection> resultado = new ArrayList<>();
                    for (int desde = 0; desde < ids.size(); desde += TAMANO_BLOQUE_IN) {
                        final List<UUID> bloque = ids.subList(desde, Math.min(ids.size(), desde + TAMANO_BLOQUE_IN));
                        entityManager.createQuery(HQL_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES, AsistenciaSesionEstudianteQueryRow.class)
                                .setParameter("idEstudiante", idEstudiante)
                                .setParameter("idAsignatura", idAsignatura)
                                .setParameter("idsSesion", bloque)
                                .getResultList()
                                .forEach(row -> resultado.add(new AsistenciaSesionEstudianteProjection(row.idSesion(),
                                        row.codigoDetalle(), row.asistio(), row.codigoRazonCausa(), row.idSolicitud(),
                                        row.estadoSolicitud(), row.fechaSolicitud())));
                    }
                    return List.copyOf(resultado);
                }
        );
    }
}
