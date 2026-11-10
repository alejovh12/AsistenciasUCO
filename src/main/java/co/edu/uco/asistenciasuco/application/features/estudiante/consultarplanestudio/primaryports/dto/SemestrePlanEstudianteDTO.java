package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto;

import java.util.List;
import java.util.UUID;

/** HU018: semestre del plan con sus asignaturas y sus totales (un semestre sin asignaturas trae la lista vacia). */
public record SemestrePlanEstudianteDTO(UUID idSemestrePlanEstudio, Integer numeroSemestre, int totalAsignaturas,
                                        int totalCreditos, List<AsignaturaPlanEstudianteDTO> asignaturas) {
}
