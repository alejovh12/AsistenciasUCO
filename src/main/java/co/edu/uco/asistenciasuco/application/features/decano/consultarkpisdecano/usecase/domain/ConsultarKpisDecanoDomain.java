package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain;

import java.util.UUID;

public record ConsultarKpisDecanoDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
