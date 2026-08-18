package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.mapper.ConsultarDocentesMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Interactor del puerto de entrada para consultar docentes.
 */
public final class ConsultarDocentesInteractor implements ConsultarDocentesInputPort {

    private final ConsultarDocentesUseCase useCase;

    public ConsultarDocentesInteractor(final ConsultarDocentesUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarDocentesUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public List<DocenteIdentidadDTO> execute() {
        return ConsultarDocentesMapper.toDTOs(useCase.execute());
    }
}
