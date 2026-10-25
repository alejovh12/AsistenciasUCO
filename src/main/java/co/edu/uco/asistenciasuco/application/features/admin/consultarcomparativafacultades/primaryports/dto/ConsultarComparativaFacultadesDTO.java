package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto;

import java.util.UUID;

public record ConsultarComparativaFacultadesDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico
) {
}
