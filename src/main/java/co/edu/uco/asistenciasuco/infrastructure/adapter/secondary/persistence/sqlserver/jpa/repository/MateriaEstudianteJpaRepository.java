package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MateriaEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consulta JPA-only de las materias del estudiante sobre grupos y asignaturas (LB-008 JPA-05). */
@Repository
public class MateriaEstudianteJpaRepository implements MateriaEstudianteQueryPort {

    static final String HQL_POR_ESTUDIANTE = """
            select distinct new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MateriaEstudianteQueryRow(
                a.id, a.nombre, g.id, g.nombre)
            from UvEstudianteGrupoEntity eg, UvGrupoEntity g, UvAsignaturaEntity a
            where g.id = eg.idGrupo
              and a.id = g.idAsignatura
              and eg.idEstudiante = :idEstudiante
            order by a.nombre, g.nombre
            """;

    private final EntityManager entityManager;

    public MateriaEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de materias de estudiante es obligatorio.");
    }

    @Override
    public List<MateriaEstudianteProjection> consultarMateriasEstudiante(final UUID idEstudiante) {
        return JpaQueryExecutor.execute(
                "consultarMateriasEstudiante",
                "No fue posible consultar las materias del estudiante desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ESTUDIANTE, MateriaEstudianteQueryRow.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toMateriaEstudiante)
                        .toList()
        );
    }
}
