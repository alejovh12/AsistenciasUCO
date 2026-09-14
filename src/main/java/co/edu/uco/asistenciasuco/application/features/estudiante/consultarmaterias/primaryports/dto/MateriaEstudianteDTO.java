package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto;

import java.util.UUID;

public record MateriaEstudianteDTO(UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo) {
}
