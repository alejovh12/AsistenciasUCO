package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase;

import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain.PerfilUsuarioDomain;

import java.util.UUID;

public interface ConsultarPerfilUseCase {

    PerfilUsuarioDomain execute(UUID actorUsuarioId);
}
