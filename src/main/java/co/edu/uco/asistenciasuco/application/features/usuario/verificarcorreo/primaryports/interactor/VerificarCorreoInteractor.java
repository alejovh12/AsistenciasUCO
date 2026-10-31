package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.VerificarCorreoInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.VerificarCorreoUseCase;

import java.util.Objects;

public final class VerificarCorreoInteractor implements VerificarCorreoInputPort {

    private final VerificarCorreoUseCase useCase;

    public VerificarCorreoInteractor(final VerificarCorreoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public String execute(final String token) {
        return useCase.execute(token);
    }
}
