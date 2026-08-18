package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.mapper.ConsultarGruposMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Interactor del puerto de entrada para consultar grupos.
 */
public final class ConsultarGruposInteractor implements ConsultarGruposInputPort {

    private final ConsultarGruposUseCase useCase;

    public ConsultarGruposInteractor(final ConsultarGruposUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarGruposUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public List<GrupoDTO> execute() {
        return ConsultarGruposMapper.toDTOs(useCase.execute());
    }
}
