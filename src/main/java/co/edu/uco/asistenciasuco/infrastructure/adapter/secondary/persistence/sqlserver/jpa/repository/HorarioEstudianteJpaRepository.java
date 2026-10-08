package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta JPA-only del horario del estudiante sobre {@code uv_horario_estudiante} (LB-008 JPA-05).
 * Horas academicas LOCALES: no aplica el mapper UTC de Sesion.
 */
@Repository
public class HorarioEstudianteJpaRepository implements HorarioEstudianteQueryPort {

    static final String HQL_POR_ESTUDIANTE = """
            select h
            from UvHorarioEstudianteEntity h
            where h.idEstudiante = :idEstudiante
            order by h.dia, h.horaInicio, h.nombreMateria
            """;

    private final EntityManager entityManager;

    public HorarioEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de horario estudiante es obligatorio.");
    }

    @Override
    public List<HorarioEstudianteProjection> consultarHorarioEstudiante(final UUID idEstudiante) {
        return JpaQueryExecutor.execute(
                "consultarHorarioEstudiante",
                "No fue posible consultar el horario del estudiante desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ESTUDIANTE, UvHorarioEstudianteEntity.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toHorarioEstudiante)
                        .toList()
        );
    }
}
