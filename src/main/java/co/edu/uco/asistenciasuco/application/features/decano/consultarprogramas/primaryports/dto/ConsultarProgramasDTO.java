package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto;

import java.util.UUID;

public record ConsultarProgramasDTO(
        UUID actorUsuarioId, String nombre, Boolean estaActivo, UUID idTipoPrograma, Integer pagina, Integer tamano
) {
}
