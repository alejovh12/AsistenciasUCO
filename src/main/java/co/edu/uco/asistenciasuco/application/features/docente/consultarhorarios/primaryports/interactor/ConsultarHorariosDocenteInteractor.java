package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.mapper.ConsultarHorariosDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarHorariosDocenteInteractor implements ConsultarHorariosDocenteInputPort {
    private final ConsultarHorariosDocenteUseCase useCase;
    public ConsultarHorariosDocenteInteractor(final ConsultarHorariosDocenteUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<HorarioDocenteDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarHorariosDocenteMapper::toDTO).toList();
    }
}
