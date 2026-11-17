package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenMateriasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenMateriasEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resumen del tablero del estudiante (CIERRE-W2B) sobre {@code usp_consultar_resumen_materias_estudiante}: una sola fila con el
 * total de materias, las materias en riesgo, las horas de falta y los creditos, calculados en la base con el mismo conjunto de
 * materias que {@code usp_consultar_materias_estudiante}. El estudiante sale del usuario ejecutor.
 */
@Repository
public class ResumenMateriasEstudianteJpaRepository implements ResumenMateriasEstudianteQueryPort {

    static final String SQL_RESUMEN = """
            EXEC dbo.usp_consultar_resumen_materias_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    static final int COLUMNAS_RESUMEN = 4;

    private final EntityManager entityManager;

    public ResumenMateriasEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del resumen de materias es obligatorio.");
    }

    @Override
    public ResumenMateriasEstudianteProjection consultarResumen(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor del resumen de materias es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarResumenMateriasEstudiante",
                "No fue posible consultar el resumen de materias del estudiante desde base de datos.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_RESUMEN)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .getResultList();
                    if (filas.isEmpty()) {
                        throw new IllegalStateException("El resumen de materias del estudiante no devolvio la fila esperada.");
                    }
                    return toProjection((Object[]) filas.getFirst());
                },
                DbCodeTranslator::traducir
        );
    }

    private static ResumenMateriasEstudianteProjection toProjection(final Object[] f) {
        if (f.length != COLUMNAS_RESUMEN) {
            throw new IllegalStateException("El resumen de materias devolvio " + f.length + " columnas; se esperaban "
                    + COLUMNAS_RESUMEN + ".");
        }
        return new ResumenMateriasEstudianteProjection(NativeRowValues.entero(f[0]), f[1] == null ? null : NativeRowValues.entero(f[1]),
                NativeRowValues.decimal2(f[2]), NativeRowValues.entero(f[3]));
    }
}
