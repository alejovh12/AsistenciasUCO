package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaTomaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MinutosSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaSesionEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaTomaEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MinutosSesionEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

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

    /** PC (puntos de control): tomas de las sesiones con su estado, reclamabilidad y solicitud; mismo aislamiento por estudiante. */
    static final String HQL_TOMAS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaTomaEstudianteQueryRow(
                t.idSesion, t.idToma, t.numeroToma, t.ordenPuntoControl, t.fechaPuntoControlUtc, t.codigoEstadoTomaEstudiante,
                t.esReclamable, t.idSolicitudRevision, t.codigoEstadoSolicitud, t.codigoResultadoAprobacion)
            from UvAsistenciaTomaEstudianteEntity t
            join UvEstudianteGrupoEntity eg on eg.id = t.idEstudianteGrupo
            join UvGrupoEntity g on g.id = eg.idGrupo
            where eg.idEstudiante = :idEstudiante
              and g.idAsignatura = :idAsignatura
              and t.idSesion in :idsSesion
            order by t.idSesion, t.ordenPuntoControl, t.numeroToma
            """;

    /** PC (puntos de control): minutos y estado de cada sesion del estudiante; mismo aislamiento por estudiante y asignatura. */
    static final String HQL_MINUTOS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MinutosSesionEstudianteQueryRow(
                h.idSesion, h.minutosSesion, h.minutosAsistidos, h.minutosFalta, h.minutosExcusa, h.minutosSinRegistro,
                h.codigoEstadoSesionEstudiante)
            from UvAsistenciaSesionHorasEntity h
            join UvEstudianteGrupoEntity eg on eg.id = h.idEstudianteGrupo
            join UvGrupoEntity g on g.id = eg.idGrupo
            where eg.idEstudiante = :idEstudiante
              and g.idAsignatura = :idAsignatura
              and h.idSesion in :idsSesion
            """;

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

    @Override
    public List<AsistenciaTomaEstudianteProjection> consultarTomasSesiones(final UUID idEstudiante, final UUID idAsignatura,
                                                                           final Collection<UUID> idsSesion) {
        return porBloques("consultarTomasSesionesEstudiante", "No fue posible consultar las tomas de las sesiones desde base de datos.",
                HQL_TOMAS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES, AsistenciaTomaEstudianteQueryRow.class, idEstudiante, idAsignatura,
                idsSesion, row -> new AsistenciaTomaEstudianteProjection(row.idSesion(), row.idToma(), row.numeroToma(),
                        row.ordenPuntoControl(), NativeRowValues.instanteUtc(row.fechaPuntoControlUtc()), row.codigoEstadoToma(),
                        Boolean.TRUE.equals(row.esReclamable()), row.idSolicitud(), row.codigoEstadoSolicitud(),
                        row.codigoResultadoAprobacion()));
    }

    @Override
    public List<MinutosSesionEstudianteProjection> consultarMinutosSesiones(final UUID idEstudiante, final UUID idAsignatura,
                                                                            final Collection<UUID> idsSesion) {
        return porBloques("consultarMinutosSesionesEstudiante", "No fue posible consultar los minutos de las sesiones desde base de datos.",
                HQL_MINUTOS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES, MinutosSesionEstudianteQueryRow.class, idEstudiante, idAsignatura,
                idsSesion, row -> new MinutosSesionEstudianteProjection(row.idSesion(), NativeRowValues.entero(row.minutosSesion()),
                        NativeRowValues.entero(row.minutosAsistidos()), NativeRowValues.entero(row.minutosFalta()),
                        NativeRowValues.entero(row.minutosExcusa()), NativeRowValues.entero(row.minutosSinRegistro()),
                        row.codigoEstadoSesion()));
    }

    /** Ejecuta la consulta acotada por estudiante, asignatura y sesiones, troceando el {@code IN} en bloques. */
    private <R, T> List<T> porBloques(final String operacion, final String mensaje, final String hql, final Class<R> tipoFila,
                                      final UUID idEstudiante, final UUID idAsignatura, final Collection<UUID> idsSesion,
                                      final Function<R, T> mapeo) {
        if (idsSesion == null || idsSesion.isEmpty()) {
            return List.of();
        }
        final List<UUID> ids = idsSesion.stream().filter(Objects::nonNull).distinct().toList();
        return JpaQueryExecutor.execute(operacion, mensaje, () -> {
            final List<T> resultado = new ArrayList<>();
            for (int desde = 0; desde < ids.size(); desde += TAMANO_BLOQUE_IN) {
                final List<UUID> bloque = ids.subList(desde, Math.min(ids.size(), desde + TAMANO_BLOQUE_IN));
                entityManager.createQuery(hql, tipoFila)
                        .setParameter("idEstudiante", idEstudiante)
                        .setParameter("idAsignatura", idAsignatura)
                        .setParameter("idsSesion", bloque)
                        .getResultList()
                        .forEach(row -> resultado.add(mapeo.apply(row)));
            }
            return List.copyOf(resultado);
        });
    }
}
