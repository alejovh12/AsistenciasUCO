package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorasProgramadasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorasProgramadasEstudianteProjection;
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
 * HU019: horas programadas de las materias del estudiante sobre {@code usp_consultar_horas_programadas_estudiante}. El SP
 * resuelve el estudiante desde el usuario ejecutor y solo cuenta la matricula inscrita. Cada columna se lee por NOMBRE
 * ({@link Tuple}); las horas salen con 2 decimales (HALF_UP), igual que los indicadores por horas.
 */
@Repository
public class HorasProgramadasEstudianteJpaRepository implements HorasProgramadasEstudianteQueryPort {

    static final String SQL_HORAS_PROGRAMADAS = """
            EXEC dbo.usp_consultar_horas_programadas_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public HorasProgramadasEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de horas programadas es obligatorio.");
    }

    @Override
    public List<HorasProgramadasEstudianteProjection> consultarHorasProgramadas(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de horas programadas es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarHorasProgramadasEstudiante",
                "No fue posible consultar las horas programadas del estudiante desde base de datos.",
                () -> tuplas(usuarioEjecutor, correlationId).stream()
                        .map(HorasProgramadasEstudianteJpaRepository::toProjection)
                        .toList(),
                DbCodeTranslator::traducir
        );
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final UUID usuarioEjecutor, final UUID correlationId) {
        return (List<Tuple>) entityManager.createNativeQuery(SQL_HORAS_PROGRAMADAS, Tuple.class)
                .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                .setParameter("idCorrelacion", correlationId)
                .getResultList();
    }

    static HorasProgramadasEstudianteProjection toProjection(final Tuple fila) {
        return new HorasProgramadasEstudianteProjection(
                NativeRowValues.uuid(fila.get("idGrupo")),
                NativeRowValues.entero(fila.get("sesionesProgramadas")),
                NativeRowValues.decimal2(fila.get("horasProgramadas")));
    }
}
