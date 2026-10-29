package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports;

import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.PerfilUsuarioDTO;

import java.util.UUID;

public interface ConsultarPerfilInputPort {

    PerfilUsuarioDTO execute(UUID actorUsuarioId);
}
