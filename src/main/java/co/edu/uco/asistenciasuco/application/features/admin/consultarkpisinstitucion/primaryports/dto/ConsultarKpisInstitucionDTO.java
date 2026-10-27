package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto;

import java.util.UUID;

public record ConsultarKpisInstitucionDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
