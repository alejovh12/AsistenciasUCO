package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU018: plan de estudios por semestres del estudiante sobre {@code usp_consultar_plan_estudio_estudiante}. El SP resuelve
 * el estudiante desde el usuario ejecutor y solo devuelve los planes activos de sus propios programas, ya ordenados. Cada
 * columna se lee por NOMBRE ({@link Tuple}); un semestre sin asignaturas trae las columnas de la asignatura en nulo.
 */
@Repository
public class PlanEstudioEstudianteJpaRepository implements PlanEstudioEstudianteQueryPort {

    static final String SQL_PLAN_ESTUDIO = """
            EXEC dbo.usp_consultar_plan_estudio_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public PlanEstudioEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del plan de estudios del estudiante es obligatorio.");
    }

    @Override
    public List<AsignaturaPlanEstudianteProjection> consultarPlanEstudio(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del plan de estudios es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarPlanEstudioEstudiante",
                "No fue posible consultar el plan de estudios del estudiante desde base de datos.",
                () -> tuplas(usuarioEjecutor, correlationId).stream()
                        .map(PlanEstudioEstudianteJpaRepository::toProjection)
                        .toList(),
                DbCodeTranslator::traducir
        );
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final UUID usuarioEjecutor, final UUID correlationId) {
        return (List<Tuple>) entityManager.createNativeQuery(SQL_PLAN_ESTUDIO, Tuple.class)
                .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                .setParameter("idCorrelacion", correlationId)
                .getResultList();
    }

    static AsignaturaPlanEstudianteProjection toProjection(final Tuple fila) {
        return new AsignaturaPlanEstudianteProjection(
                NativeRowValues.uuid(fila.get("idPrograma")),
                NativeRowValues.texto(fila.get("nombrePrograma")),
                NativeRowValues.uuid(fila.get("idPlanEstudio")),
                enteroONulo(fila.get("inpPlanEstudio")),
                NativeRowValues.uuid(fila.get("idSemestrePlanEstudio")),
                enteroONulo(fila.get("numeroSemestre")),
                NativeRowValues.uuid(fila.get("idAsignatura")),
                NativeRowValues.texto(fila.get("codigoAsignatura")),
                NativeRowValues.texto(fila.get("nombreAsignatura")),
                enteroONulo(fila.get("creditos")),
                NativeRowValues.texto(fila.get("codigoComponente")),
                NativeRowValues.texto(fila.get("codigoCaracter")),
                NativeRowValues.texto(fila.get("nombreCaracter")),
                NativeRowValues.bit(fila.get("cursando")),
                NativeRowValues.uuid(fila.get("idGrupoCursando")),
                NativeRowValues.texto(fila.get("nombreGrupoCursando")));
    }

    private static Integer enteroONulo(final Object valor) {
        return valor == null ? null : NativeRowValues.entero(valor);
    }
}
