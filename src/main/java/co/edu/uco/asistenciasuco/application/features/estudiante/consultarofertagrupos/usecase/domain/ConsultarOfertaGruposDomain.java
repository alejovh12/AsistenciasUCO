package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain;

import java.util.UUID;

public record ConsultarOfertaGruposDomain(
        UUID actorUsuarioId,
        String busqueda,
        Integer pagina,
        Integer tamano
) {
}
