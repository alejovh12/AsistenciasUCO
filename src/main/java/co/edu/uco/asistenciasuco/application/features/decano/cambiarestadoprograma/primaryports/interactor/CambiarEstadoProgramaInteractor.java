package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.CambiarEstadoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.mapper.CambiarEstadoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.CambiarEstadoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import java.util.Objects;

public class CambiarEstadoProgramaInteractor implements CambiarEstadoProgramaInputPort {

    private final CambiarEstadoProgramaUseCase useCase;

    public CambiarEstadoProgramaInteractor(final CambiarEstadoProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ProgramaAcademicoDTO execute(final CambiarEstadoProgramaDTO dto) {
        return CambiarEstadoProgramaMapper.toDto(useCase.execute(CambiarEstadoProgramaMapper.toDomain(dto)));
    }
}
