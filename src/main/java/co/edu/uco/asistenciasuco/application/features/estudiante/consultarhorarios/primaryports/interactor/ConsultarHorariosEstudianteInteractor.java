package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.mapper.ConsultarHorariosEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarHorariosEstudianteInteractor implements ConsultarHorariosEstudianteInputPort {
    private final ConsultarHorariosEstudianteUseCase useCase;
    public ConsultarHorariosEstudianteInteractor(final ConsultarHorariosEstudianteUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<HorarioEstudianteDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarHorariosEstudianteMapper::toDTO).toList();
    }
}
