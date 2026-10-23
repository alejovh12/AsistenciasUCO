package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoVinculacionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoVinculacionProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoVinculacionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class TipoVinculacionJpaRepository implements TipoVinculacionQueryPort {

    static final String HQL_ACTIVOS = """
            select t
            from UvTipoVinculacionEntity t
            where t.estaActivoTipoVinculacion = true
            order by t.nombre
            """;

    private final EntityManager entityManager;

    public TipoVinculacionJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de tipos de vinculacion es obligatorio.");
    }

    @Override
    public List<TipoVinculacionProjection> consultarActivos() {
        return JpaQueryExecutor.execute(
                "consultarTiposVinculacion",
                "No fue posible consultar los tipos de vinculacion.",
                () -> entityManager.createQuery(HQL_ACTIVOS, UvTipoVinculacionEntity.class).getResultList().stream()
                        .map(t -> new TipoVinculacionProjection(t.id(), t.codigo(), t.nombre())).toList()
        );
    }
}
