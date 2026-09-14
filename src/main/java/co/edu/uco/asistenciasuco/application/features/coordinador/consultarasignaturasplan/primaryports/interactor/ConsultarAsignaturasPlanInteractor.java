package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.mapper.ConsultarAsignaturasPlanMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.ConsultarAsignaturasPlanUseCase;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarAsignaturasPlanInteractor implements ConsultarAsignaturasPlanInputPort {
    private final ConsultarAsignaturasPlanUseCase useCase;
    public ConsultarAsignaturasPlanInteractor(final ConsultarAsignaturasPlanUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<AsignaturaDTO> execute(final UUID actorUsuarioId, final UUID idPlanEstudio) { return useCase.execute(actorUsuarioId, idPlanEstudio).stream().map(ConsultarAsignaturasPlanMapper::toDTO).toList(); }
}
