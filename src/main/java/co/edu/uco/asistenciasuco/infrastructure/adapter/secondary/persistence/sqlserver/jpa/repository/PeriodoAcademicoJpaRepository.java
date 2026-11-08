package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPeriodosAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAdminProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
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

    static final String SQL_PERIODOS_ADMIN = """
            EXEC dbo.usp_consultar_periodos_academicos_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final int COLUMNAS_PERIODO_ADMIN = 10;

    @Override
    public PaginaPeriodosAdminProjection consultarPeriodosAdministrador(final UUID idUsuarioEjecutor, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarPeriodosAdministrador", "No fue posible consultar los periodos del administrador.", () -> {
            final List<?> filas = entityManager.createNativeQuery(SQL_PERIODOS_ADMIN)
                    .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                    .setParameter("idCorrelacion", correlationId)
                    .setParameter("numeroPagina", pagina)
                    .setParameter("tamanoPagina", tamano)
                    .getResultList();
            long total = 0;
            final List<PeriodoAdminProjection> periodos = new java.util.ArrayList<>();
            for (final Object fila : filas) {
                final Object[] c = (Object[]) fila;
                if (c.length != COLUMNAS_PERIODO_ADMIN) {
                    throw new IllegalStateException("usp_consultar_periodos_academicos_paginado debe devolver " + COLUMNAS_PERIODO_ADMIN + " columnas.");
                }
                periodos.add(new PeriodoAdminProjection(NativeRowValues.uuid(c[0]), NativeRowValues.uuid(c[1]), NativeRowValues.texto(c[2]),
                        NativeRowValues.texto(c[3]), String.valueOf(c[4]), fecha(c[5]), fecha(c[6]), NativeRowValues.entero(c[7]),
                        NativeRowValues.bit(c[8])));
                total = NativeRowValues.entero(c[9]);
            }
            return new PaginaPeriodosAdminProjection(periodos, total);
        }, DbCodeTranslator::traducir);
    }

    private static java.time.LocalDate fecha(final Object valor) {
        if (valor instanceof java.time.LocalDate fecha) {
            return fecha;
        }
        return valor == null ? null : java.time.LocalDate.parse(valor.toString().substring(0, 10));
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
