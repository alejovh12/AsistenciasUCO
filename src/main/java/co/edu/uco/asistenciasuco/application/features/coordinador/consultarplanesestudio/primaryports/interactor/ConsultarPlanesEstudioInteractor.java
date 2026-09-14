package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.ConsultarPlanesEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.mapper.ConsultarPlanesEstudioMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.ConsultarPlanesEstudioUseCase;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarPlanesEstudioInteractor implements ConsultarPlanesEstudioInputPort {
    private final ConsultarPlanesEstudioUseCase useCase;
    public ConsultarPlanesEstudioInteractor(final ConsultarPlanesEstudioUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<PlanEstudioDTO> execute(final UUID actorUsuarioId) { return useCase.execute(actorUsuarioId).stream().map(ConsultarPlanesEstudioMapper::toDTO).toList(); }
}
