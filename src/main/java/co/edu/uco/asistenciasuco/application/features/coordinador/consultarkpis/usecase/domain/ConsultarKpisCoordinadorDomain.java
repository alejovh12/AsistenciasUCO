package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain;

import java.util.UUID;

public record ConsultarKpisCoordinadorDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
