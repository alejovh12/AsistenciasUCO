package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain;

import java.util.UUID;

public record ConsultarProgramasDomain(
        UUID actorUsuarioId, String nombre, Boolean estaActivo, UUID idTipoPrograma, Integer pagina, Integer tamano
) {
}
