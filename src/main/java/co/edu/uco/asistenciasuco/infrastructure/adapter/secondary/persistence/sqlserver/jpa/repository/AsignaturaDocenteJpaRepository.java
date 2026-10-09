package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consulta JPA-only de la asignacion academica del docente sobre {@code uv_docente} (LB-008 JPA-05). */
@Repository
public class AsignaturaDocenteJpaRepository implements AsignaturaDocenteQueryPort {

    static final String HQL_POR_DOCENTE = """
            select distinct d
            from UvDocenteEntity d
            where d.id = :idDocente
            order by d.nombreAsignatura, d.nombreGrupo
            """;

    private final EntityManager entityManager;

    public AsignaturaDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de asignaturas de docente es obligatorio.");
    }

    @Override
    public List<AsignaturaDocenteProjection> consultarAsignaturasDocente(final UUID idDocente) {
        return JpaQueryExecutor.execute(
                "consultarAsignaturasDocente",
                "No fue posible consultar las asignaturas del docente desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_DOCENTE, UvDocenteEntity.class)
                        .setParameter("idDocente", idDocente)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toAsignaturaDocente)
                        .toList()
        );
    }
}
