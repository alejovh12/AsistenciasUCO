package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.CambiarEstadoTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.mapper.CambiarEstadoTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.CambiarEstadoTipoProgramaUseCase;

import java.util.Objects;

public class CambiarEstadoTipoProgramaInteractor implements CambiarEstadoTipoProgramaInputPort {

    private final CambiarEstadoTipoProgramaUseCase useCase;

    public CambiarEstadoTipoProgramaInteractor(final CambiarEstadoTipoProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoTipoProgramaDTO dto) {
        useCase.execute(CambiarEstadoTipoProgramaMapper.toDomain(dto));
    }
}
