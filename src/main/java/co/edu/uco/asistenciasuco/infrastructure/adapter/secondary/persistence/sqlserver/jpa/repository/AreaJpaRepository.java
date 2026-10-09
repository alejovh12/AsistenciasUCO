package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAreaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

/** Consulta JPA-only de {@code uv_area} (LB-008 JPA-05). */
@Repository
public class AreaJpaRepository implements AreaQueryPort {

    static final String HQL_LISTAR = """
            select a
            from UvAreaEntity a
            order by a.nombre, a.id
            """;

    private final EntityManager entityManager;

    public AreaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de areas es obligatorio.");
    }

    @Override
    public List<AreaProjection> consultarAreas() {
        return JpaQueryExecutor.execute(
                "consultarAreas",
                "No fue posible consultar las areas desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvAreaEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toArea)
                        .toList()
        );
    }
}
