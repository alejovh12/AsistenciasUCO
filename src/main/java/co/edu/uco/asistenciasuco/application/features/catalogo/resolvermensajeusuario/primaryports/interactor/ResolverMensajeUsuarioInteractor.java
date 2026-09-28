package co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;

import java.util.Objects;
import java.util.Optional;

/**
 * Interactor del puerto de entrada para resolver mensajes de usuario por codigo.
 */
public final class ResolverMensajeUsuarioInteractor implements ResolverMensajeUsuarioInputPort {

    private final ResolverMensajeUsuarioUseCase useCase;

    public ResolverMensajeUsuarioInteractor(final ResolverMensajeUsuarioUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ResolverMensajeUsuarioUseCase es obligatorio.");
    }

    @Override
    public Optional<String> execute(final String codigo) {
        return useCase.execute(codigo);
    }
}
