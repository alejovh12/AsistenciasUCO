package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.mapper.ConsultarEstudiantesProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.ConsultarEstudiantesProgramaUseCase;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarEstudiantesProgramaInteractor implements ConsultarEstudiantesProgramaInputPort {
    private final ConsultarEstudiantesProgramaUseCase useCase;
    public ConsultarEstudiantesProgramaInteractor(final ConsultarEstudiantesProgramaUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<EstudianteProgramaDTO> execute(final UUID actorUsuarioId) { return useCase.execute(actorUsuarioId).stream().map(ConsultarEstudiantesProgramaMapper::toDTO).toList(); }
}
