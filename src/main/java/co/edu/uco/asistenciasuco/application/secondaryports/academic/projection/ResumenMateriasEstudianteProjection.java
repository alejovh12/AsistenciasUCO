package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;

/** Fila unica de {@code usp_consultar_resumen_materias_estudiante}; {@code materiasEnRiesgo} nulo = sin umbrales configurados. */
public record ResumenMateriasEstudianteProjection(int totalMaterias, Integer materiasEnRiesgo, BigDecimal totalHorasFalta, int totalCreditos) {
}
