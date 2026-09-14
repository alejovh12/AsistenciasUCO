package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.RegistrarAsistenciaAutonomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.mapper.RegistrarAsistenciaAutonomaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.RegistrarAsistenciaAutonomaUseCase;

import java.util.Objects;

public final class RegistrarAsistenciaAutonomaInteractor implements RegistrarAsistenciaAutonomaInputPort {

    private final RegistrarAsistenciaAutonomaUseCase useCase;

    public RegistrarAsistenciaAutonomaInteractor(final RegistrarAsistenciaAutonomaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "RegistrarAsistenciaAutonomaUseCase es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciaAutonomaDTO dto) {
        useCase.execute(RegistrarAsistenciaAutonomaMapper.toDomain(dto));
    }
}
