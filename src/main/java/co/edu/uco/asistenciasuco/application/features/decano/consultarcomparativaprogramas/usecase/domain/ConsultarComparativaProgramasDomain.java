package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain;

import java.util.UUID;

public record ConsultarComparativaProgramasDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
