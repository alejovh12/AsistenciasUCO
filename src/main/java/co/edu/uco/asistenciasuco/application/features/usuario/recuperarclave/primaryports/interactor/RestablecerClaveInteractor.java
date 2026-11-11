package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.RestablecerClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.RestablecerClaveUseCase;

import java.util.Objects;

public final class RestablecerClaveInteractor implements RestablecerClaveInputPort {

    private final RestablecerClaveUseCase useCase;

    public RestablecerClaveInteractor(final RestablecerClaveUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso para definir la contrasena es obligatorio.");
    }

    @Override
    public String execute(final String token, final String claveNueva) {
        return useCase.execute(token, claveNueva);
    }
}
