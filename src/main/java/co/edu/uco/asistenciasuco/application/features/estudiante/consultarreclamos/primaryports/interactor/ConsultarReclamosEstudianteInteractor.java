package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.ConsultarReclamosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.mapper.ConsultarReclamosEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.ConsultarReclamosEstudianteUseCase;

import java.util.Objects;

public final class ConsultarReclamosEstudianteInteractor implements ConsultarReclamosEstudianteInputPort {

    private final ConsultarReclamosEstudianteUseCase useCase;

    public ConsultarReclamosEstudianteInteractor(final ConsultarReclamosEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ReclamosEstudiantePaginaDTO execute(final ConsultarReclamosEstudianteDTO dto) {
        return ConsultarReclamosEstudianteMapper.toDTO(useCase.execute(ConsultarReclamosEstudianteMapper.toDomain(dto)));
    }
}
