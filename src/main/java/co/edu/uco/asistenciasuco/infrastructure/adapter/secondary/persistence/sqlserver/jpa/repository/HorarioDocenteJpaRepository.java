package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta JPA-only del horario del docente sobre {@code uv_horario_docente} (LB-008 JPA-05).
 * Horas academicas LOCALES: no aplica el mapper UTC de Sesion.
 */
@Repository
public class HorarioDocenteJpaRepository implements HorarioDocenteQueryPort {

    static final String HQL_POR_DOCENTE = """
            select h
            from UvHorarioDocenteEntity h
            where h.idDocente = :idDocente
            order by h.dia, h.horaInicio, h.nombreMateria
            """;

    private final EntityManager entityManager;

    public HorarioDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de horario docente es obligatorio.");
    }

    @Override
    public List<HorarioDocenteProjection> consultarHorarioDocente(final UUID idDocente) {
        return JpaQueryExecutor.execute(
                "consultarHorarioDocente",
                "No fue posible consultar el horario del docente desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_DOCENTE, UvHorarioDocenteEntity.class)
                        .setParameter("idDocente", idDocente)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toHorarioDocente)
                        .toList()
        );
    }
}
