package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.ReenviarVerificacionCorreoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.ReenviarVerificacionCorreoUseCase;

import java.util.Objects;
import java.util.UUID;

public final class ReenviarVerificacionCorreoInteractor implements ReenviarVerificacionCorreoInputPort {

    private final ReenviarVerificacionCorreoUseCase useCase;

    public ReenviarVerificacionCorreoInteractor(final ReenviarVerificacionCorreoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final UUID actorUsuarioId, final UUID grupoId, final UUID estudianteId) {
        useCase.execute(actorUsuarioId, grupoId, estudianteId);
    }
}
