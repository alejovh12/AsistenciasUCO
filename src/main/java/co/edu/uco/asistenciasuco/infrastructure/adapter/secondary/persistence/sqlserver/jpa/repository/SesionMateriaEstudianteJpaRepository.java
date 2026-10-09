package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.SesionMateriaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta JPA-only de sesiones de una asignatura para el estudiante (LB-008 JPA-05).
 * Las fechas de {@code uv_sesion} conservan la semantica UTC certificada en JPA-04.
 */
@Repository
public class SesionMateriaEstudianteJpaRepository implements SesionMateriaEstudianteQueryPort {

    static final String HQL_POR_ESTUDIANTE_Y_ASIGNATURA = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.SesionMateriaQueryRow(
                s.id, s.nombre, s.numero, s.codigo, s.numeroSemana, s.idGrupo, s.codigoGrupo, s.nombreGrupo,
                s.fechaHoraInicio, s.fechaHoraFin)
            from UvSesionEntity s, UvEstudianteGrupoEntity eg, UvGrupoEntity g
            where eg.idGrupo = s.idGrupo
              and g.id = s.idGrupo
              and eg.idEstudiante = :idEstudiante
              and g.idAsignatura = :idAsignatura
            order by s.numero, s.fechaHoraInicio
            """;

    private final EntityManager entityManager;

    public SesionMateriaEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de sesiones de materia es obligatorio.");
    }

    @Override
    public List<SesionMateriaEstudianteProjection> consultarSesionesMateria(final UUID idEstudiante,
                                                                           final UUID idAsignatura) {
        return JpaQueryExecutor.execute(
                "consultarSesionesMateria",
                "No fue posible consultar las sesiones de la materia desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ESTUDIANTE_Y_ASIGNATURA, SesionMateriaQueryRow.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .setParameter("idAsignatura", idAsignatura)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toSesionMateria)
                        .toList()
        );
    }
}
