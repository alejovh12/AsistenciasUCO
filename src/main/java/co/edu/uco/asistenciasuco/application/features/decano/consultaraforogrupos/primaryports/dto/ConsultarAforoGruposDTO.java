package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto;

import java.util.UUID;

public record ConsultarAforoGruposDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        UUID idPrograma,
        Boolean soloHabilitados,
        Integer pagina,
        Integer tamano
) {
}
