package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.ActualizarProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.mapper.ActualizarProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.ActualizarProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import java.util.Objects;

public class ActualizarProgramaInteractor implements ActualizarProgramaInputPort {

    private final ActualizarProgramaUseCase useCase;

    public ActualizarProgramaInteractor(final ActualizarProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ProgramaAcademicoDTO execute(final ActualizarProgramaDTO dto) {
        return ActualizarProgramaMapper.toDto(useCase.execute(ActualizarProgramaMapper.toDomain(dto)));
    }
}
