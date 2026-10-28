package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto;

import java.util.UUID;

public record ConsultarOfertaGruposDTO(
        UUID actorUsuarioId,
        String busqueda,
        Integer pagina,
        Integer tamano
) {
}
