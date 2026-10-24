package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto;

import java.util.UUID;

public record ConsultarComparativaProgramasDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
