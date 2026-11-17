package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto;

import java.math.BigDecimal;

/**
 * CIERRE-W2B: indicadores del tablero del estudiante, calculados en la base con el mismo conjunto de materias que el
 * listado ({@code GET /api/v1/estudiante/materias}): matricula inscrita, una fila por asignatura y grupo.
 *
 * @param totalMaterias    materias del listado
 * @param materiasEnRiesgo materias con permanencia RIESGO o PERDIDA segun la regla y los umbrales de la base; {@code null} si hay
 *                         materias pero ningun umbral esta configurado (sin dato, nunca un cero inventado)
 * @param totalHorasFalta  suma de horas de falta sin justa causa (cada materia a 2 decimales)
 * @param totalCreditos    suma de creditos de las materias del listado
 */
public record ResumenMateriasEstudianteDTO(int totalMaterias, Integer materiasEnRiesgo, BigDecimal totalHorasFalta, int totalCreditos) {
}
