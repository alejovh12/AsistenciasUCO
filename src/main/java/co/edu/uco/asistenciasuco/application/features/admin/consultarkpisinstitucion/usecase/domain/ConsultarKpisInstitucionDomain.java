package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain;

import java.util.UUID;

public record ConsultarKpisInstitucionDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
