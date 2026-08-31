package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.mapper.ConsultarEstudiantesMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import java.util.Objects;

public final class ConsultarEstudiantesInteractor implements ConsultarEstudiantesInputPort {

    private final ConsultarEstudiantesUseCase useCase;

    public ConsultarEstudiantesInteractor(final ConsultarEstudiantesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarEstudiantesUseCase es obligatorio.");
    }

    @Override
    public EstudiantePaginaDTO execute(final ConsultarEstudiantesDTO dto) {
        return ConsultarEstudiantesMapper.toDTO(useCase.execute(ConsultarEstudiantesMapper.toDomain(dto)));
    }
}