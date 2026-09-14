package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de materias del estudiante.
 */
public record MateriaEstudianteDomain(UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo) {
}
