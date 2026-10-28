package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto;

import java.util.UUID;

public record ConsultarFichaDocenteDTO(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idPeriodoAcademico,
        Integer pagina,
        Integer tamano
) {
}
