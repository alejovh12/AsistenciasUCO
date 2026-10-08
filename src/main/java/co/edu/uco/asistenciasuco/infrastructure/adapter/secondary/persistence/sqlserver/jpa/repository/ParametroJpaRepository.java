package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvParametroEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

/** Consulta JPA-only de {@code uv_parametro} (LB-008 JPA-05). El filtro {@code estaActivo = 1} vive en la vista. */
@Repository
public class ParametroJpaRepository implements ParametroQueryPort {

    static final String HQL_LISTAR = """
            select p
            from UvParametroEntity p
            order by p.grupo, p.clave, p.id
            """;

    private final EntityManager entityManager;

    public ParametroJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de parametros es obligatorio.");
    }

    @Override
    public List<ParametroProjection> consultarParametros() {
        return JpaQueryExecutor.execute(
                "consultarParametros",
                "No fue posible consultar los parametros desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvParametroEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toParametro)
                        .toList()
        );
    }
}
