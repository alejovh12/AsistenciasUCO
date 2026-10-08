package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Consulta JPA-only de {@code uv_periodo_academico} (LB-008 JPA-05). */
@Repository
public class PeriodoAcademicoJpaRepository implements PeriodoAcademicoQueryPort {

    static final String HQL_SELECT = """
            select p
            from UvPeriodoAcademicoEntity p
            """;
    static final String HQL_LISTAR = HQL_SELECT + "order by p.nombre, p.id";
    static final String HQL_POR_ID = HQL_SELECT + "where p.id = :idPeriodoAcademico";

    private final EntityManager entityManager;

    public PeriodoAcademicoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de periodos es obligatorio.");
    }

    @Override
    public List<PeriodoAcademicoProjection> consultarPeriodosAcademicos() {
        return JpaQueryExecutor.execute(
                "consultarPeriodosAcademicos",
                "No fue posible consultar los periodos desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvPeriodoAcademicoEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toPeriodoAcademico)
                        .toList()
        );
    }

    @Override
    public Optional<PeriodoAcademicoProjection> consultarPeriodoAcademicoPorId(final UUID idPeriodoAcademico) {
        return JpaQueryExecutor.execute(
                "consultarPeriodoAcademicoPorId",
                "No fue posible consultar el periodo desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvPeriodoAcademicoEntity.class)
                        .setParameter("idPeriodoAcademico", idPeriodoAcademico)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(AcademicViewJpaProjectionMapper::toPeriodoAcademico)
        );
    }
}
