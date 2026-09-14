package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.mapper.ConsultarAsignaturasDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarAsignaturasDocenteInteractor implements ConsultarAsignaturasDocenteInputPort {
    private final ConsultarAsignaturasDocenteUseCase useCase;
    public ConsultarAsignaturasDocenteInteractor(final ConsultarAsignaturasDocenteUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<AsignaturaDocenteDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarAsignaturasDocenteMapper::toDTO).toList();
    }
}
