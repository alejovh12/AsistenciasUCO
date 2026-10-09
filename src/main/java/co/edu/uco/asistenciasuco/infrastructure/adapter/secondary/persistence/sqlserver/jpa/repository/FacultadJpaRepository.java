package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Consulta JPA-only de {@code uv_facultad} (LB-008 JPA-05). {@code idDecano} puede ser NULL en la vista. */
@Repository
public class FacultadJpaRepository implements FacultadQueryPort {

    static final String HQL_SELECT = """
            select f
            from UvFacultadEntity f
            """;
    static final String HQL_LISTAR = HQL_SELECT + "order by f.nombreInstitucion, f.nombreFacultad, f.id";
    static final String HQL_POR_ID = HQL_SELECT + "where f.id = :idFacultad";

    private final EntityManager entityManager;

    public FacultadJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de facultades es obligatorio.");
    }

    @Override
    public List<FacultadProjection> consultarFacultades() {
        return JpaQueryExecutor.execute(
                "consultarFacultades",
                "No fue posible consultar las facultades desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvFacultadEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toFacultad)
                        .toList()
        );
    }

    @Override
    public Optional<FacultadProjection> consultarFacultadPorId(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarFacultadPorId",
                "No fue posible consultar la facultad desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvFacultadEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(AcademicViewJpaProjectionMapper::toFacultad)
        );
    }
}
