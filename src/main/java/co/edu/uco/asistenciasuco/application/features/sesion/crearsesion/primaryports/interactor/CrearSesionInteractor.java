package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.mapper.CrearSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para crear sesion.
 */
public final class CrearSesionInteractor implements CrearSesionInputPort {

    private final CrearSesionUseCase useCase;

    public CrearSesionInteractor(final CrearSesionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso CrearSesionUseCase es obligatorio.");
    }

    @Override
    public void execute(final CrearSesionDTO dto) {
        final CrearSesionDomain domain = CrearSesionMapper.toDomain(dto);
        useCase.execute(domain);
    }
}