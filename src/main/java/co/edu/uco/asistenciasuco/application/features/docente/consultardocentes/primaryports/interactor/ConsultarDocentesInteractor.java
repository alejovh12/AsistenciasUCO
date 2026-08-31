package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.mapper.ConsultarDocentesMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar docentes.
 */
public final class ConsultarDocentesInteractor implements ConsultarDocentesInputPort {

    private final ConsultarDocentesUseCase useCase;

    public ConsultarDocentesInteractor(final ConsultarDocentesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarDocentesUseCase es obligatorio.");
    }

    @Override
    public List<DocenteIdentidadDTO> execute() {
        return ConsultarDocentesMapper.toDTOs(useCase.execute());
    }
}