package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto;

import java.util.UUID;

public record ConsultarKpisDecanoDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
