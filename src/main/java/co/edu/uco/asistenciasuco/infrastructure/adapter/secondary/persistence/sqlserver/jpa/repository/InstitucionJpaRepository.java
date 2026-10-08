package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvInstitucionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

/** Consulta JPA-only de {@code uv_institucion} (LB-008 JPA-05). */
@Repository
public class InstitucionJpaRepository implements InstitucionQueryPort {

    static final String HQL_LISTAR = """
            select i
            from UvInstitucionEntity i
            order by i.nombre, i.id
            """;

    private final EntityManager entityManager;

    public InstitucionJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de instituciones es obligatorio.");
    }

    @Override
    public List<InstitucionProjection> consultarInstituciones() {
        return JpaQueryExecutor.execute(
                "consultarInstituciones",
                "No fue posible consultar las instituciones desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvInstitucionEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toInstitucion)
                        .toList()
        );
    }
}
