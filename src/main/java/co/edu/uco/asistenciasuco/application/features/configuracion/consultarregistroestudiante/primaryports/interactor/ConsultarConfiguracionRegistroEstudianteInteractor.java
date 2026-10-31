package co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.ConsultarConfiguracionRegistroEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.ConsultarConfiguracionRegistroEstudianteUseCase;

import java.util.Objects;

public final class ConsultarConfiguracionRegistroEstudianteInteractor implements ConsultarConfiguracionRegistroEstudianteInputPort {

    private final ConsultarConfiguracionRegistroEstudianteUseCase useCase;

    public ConsultarConfiguracionRegistroEstudianteInteractor(final ConsultarConfiguracionRegistroEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public String execute() {
        return useCase.execute();
    }
}
