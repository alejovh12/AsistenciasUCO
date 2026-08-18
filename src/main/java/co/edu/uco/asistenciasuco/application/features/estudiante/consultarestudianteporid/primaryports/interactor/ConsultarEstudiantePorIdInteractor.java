package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.mapper.ConsultarEstudiantePorIdMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.ConsultarEstudiantePorIdUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

public final class ConsultarEstudiantePorIdInteractor implements ConsultarEstudiantePorIdInputPort {

    private final ConsultarEstudiantePorIdUseCase useCase;

    public ConsultarEstudiantePorIdInteractor(final ConsultarEstudiantePorIdUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarEstudiantePorIdUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public EstudianteDetalleDTO execute(final UUID estudianteId) {
        return ConsultarEstudiantePorIdMapper.toDTO(useCase.execute(ConsultarEstudiantePorIdMapper.toDomain(estudianteId)));
    }
}
