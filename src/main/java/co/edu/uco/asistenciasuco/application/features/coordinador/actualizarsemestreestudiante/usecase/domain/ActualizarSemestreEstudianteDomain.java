package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain;

import java.util.UUID;

public record ActualizarSemestreEstudianteDomain(UUID actorUsuarioId, UUID idEstudiante, UUID idPrograma, Integer semestreActual) {
}
