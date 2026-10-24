package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.CambiarEstadoAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.mapper.CambiarEstadoAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.CambiarEstadoAreaUseCase;

import java.util.Objects;

public class CambiarEstadoAreaInteractor implements CambiarEstadoAreaInputPort {

    private final CambiarEstadoAreaUseCase useCase;

    public CambiarEstadoAreaInteractor(final CambiarEstadoAreaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoAreaDTO dto) {
        useCase.execute(CambiarEstadoAreaMapper.toDomain(dto));
    }
}
