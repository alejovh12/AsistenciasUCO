package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.mapper.ActualizarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.ActualizarSesionUseCase;

import java.util.Objects;

public final class ActualizarSesionInteractor implements ActualizarSesionInputPort {

    private final ActualizarSesionUseCase useCase;

    public ActualizarSesionInteractor(final ActualizarSesionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ActualizarSesionUseCase es obligatorio.");
    }

    @Override
    public void execute(final ActualizarSesionDTO dto) {
        useCase.execute(ActualizarSesionMapper.toDomain(dto));
    }
}
