package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain;

import java.util.UUID;

public record ConsultarComparativaFacultadesDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
