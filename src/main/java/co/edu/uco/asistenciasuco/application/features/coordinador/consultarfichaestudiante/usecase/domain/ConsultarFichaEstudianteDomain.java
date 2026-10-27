package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain;

import java.util.UUID;

public record ConsultarFichaEstudianteDomain(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico
) {
}
