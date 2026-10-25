package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto;

import java.util.UUID;

public record ConsultarKpisCoordinadorDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
