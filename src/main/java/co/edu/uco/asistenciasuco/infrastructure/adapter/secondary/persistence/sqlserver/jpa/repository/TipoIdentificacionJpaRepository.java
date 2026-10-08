package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoIdentificacionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

@Repository
public class TipoIdentificacionJpaRepository implements TipoIdentificacionRepositoryPort {
    static final String HQL_TODOS = """
            select t
            from UvTipoIdentificacionEntity t
            order by t.tipoIdentificacion
            """;
    private final EntityManager entityManager;
    public TipoIdentificacionJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de tipos de identificacion es obligatorio.");
    }
    @Override
    public List<TipoIdentificacionRepositoryProjection> consultarTiposIdentificacion() {
        return JpaQueryExecutor.execute(
                "consultarTiposIdentificacion",
                "No fue posible consultar los tipos de identificacion.",
                () -> entityManager.createQuery(HQL_TODOS, UvTipoIdentificacionEntity.class).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toTipoIdentificacion).toList()
        );
    }
}

