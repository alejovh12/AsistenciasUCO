package co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.ConsultarMensajeInterfazUseCase;

import java.util.Objects;

public final class ConsultarMensajeInterfazInteractor implements ConsultarMensajeInterfazInputPort {

    private final ConsultarMensajeInterfazUseCase useCase;

    public ConsultarMensajeInterfazInteractor(final ConsultarMensajeInterfazUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public MensajeInterfaz execute(final String codigo) {
        return useCase.execute(codigo);
    }
}
