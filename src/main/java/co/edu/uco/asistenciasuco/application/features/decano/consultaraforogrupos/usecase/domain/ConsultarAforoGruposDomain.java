package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain;

import java.util.UUID;

public record ConsultarAforoGruposDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        UUID idPrograma,
        Boolean soloHabilitados,
        Integer pagina,
        Integer tamano
) {
}
