package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto;

import java.util.UUID;

/** {@code semestreActual} nulo limpia el semestre del estudiante en el programa. */
public record ActualizarSemestreEstudianteDTO(UUID actorUsuarioId, UUID idEstudiante, UUID idPrograma, Integer semestreActual) {
}
