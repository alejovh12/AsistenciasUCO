package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.ConsultarConfiguracionReclamosInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.mapper.ConsultarConfiguracionReclamosMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.ConsultarConfiguracionReclamosUseCase;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarConfiguracionReclamosInteractor implements ConsultarConfiguracionReclamosInputPort {

    private final ConsultarConfiguracionReclamosUseCase useCase;

    public ConsultarConfiguracionReclamosInteractor(final ConsultarConfiguracionReclamosUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ConfiguracionReclamosDTO execute(final UUID actorUsuarioId) {
        return ConsultarConfiguracionReclamosMapper.toDTO(useCase.execute(actorUsuarioId));
    }
}
