package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.SolicitarRecuperacionClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.SolicitarRecuperacionClaveUseCase;

import java.util.Objects;

public final class SolicitarRecuperacionClaveInteractor implements SolicitarRecuperacionClaveInputPort {

    private final SolicitarRecuperacionClaveUseCase useCase;

    public SolicitarRecuperacionClaveInteractor(final SolicitarRecuperacionClaveUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de solicitud de recuperacion es obligatorio.");
    }

    @Override
    public String execute(final String correo) {
        return useCase.execute(correo);
    }
}
