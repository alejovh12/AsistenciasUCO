package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto;

import java.util.UUID;

public record ConsultarFichaEstudianteDTO(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico
) {
}
