package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.mapper.ConsultarEstudiantesMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

public final class ConsultarEstudiantesInteractor implements ConsultarEstudiantesInputPort {

    private final ConsultarEstudiantesUseCase useCase;

    public ConsultarEstudiantesInteractor(final ConsultarEstudiantesUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarEstudiantesUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public EstudiantePaginaDTO execute(final ConsultarEstudiantesDTO dto) {
        return ConsultarEstudiantesMapper.toDTO(useCase.execute(ConsultarEstudiantesMapper.toDomain(dto)));
    }
}
