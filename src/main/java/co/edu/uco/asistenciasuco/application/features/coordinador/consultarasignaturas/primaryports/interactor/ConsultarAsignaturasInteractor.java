package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.ConsultarAsignaturasInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.mapper.ConsultarAsignaturasMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.ConsultarAsignaturasUseCase;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarAsignaturasInteractor implements ConsultarAsignaturasInputPort {
    private final ConsultarAsignaturasUseCase useCase;
    public ConsultarAsignaturasInteractor(final ConsultarAsignaturasUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<AsignaturaDTO> execute(final UUID actorUsuarioId) { return useCase.execute(actorUsuarioId).stream().map(ConsultarAsignaturasMapper::toDTO).toList(); }
}
