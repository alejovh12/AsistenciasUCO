package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.mapper.ConsultarDocentesMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.mapper.ConsultarDocentePorIdMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.ConsultarDocentePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para consultar un docente por ID.
 */
public final class ConsultarDocentePorIdInteractor implements ConsultarDocentePorIdInputPort {

    private final ConsultarDocentePorIdUseCase useCase;

    public ConsultarDocentePorIdInteractor(final ConsultarDocentePorIdUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarDocentePorIdUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public DocenteIdentidadDTO execute(final ConsultarDocentePorIdDTO dto) {
        final ConsultarDocentePorIdDomain domain = ConsultarDocentePorIdMapper.toDomain(dto);
        return ConsultarDocentesMapper.toDTO(useCase.execute(domain));
    }
}
