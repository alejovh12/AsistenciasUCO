package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.mapper.CerrarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.CerrarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para cerrar sesion.
 */
public final class CerrarSesionInteractor implements CerrarSesionInputPort {

    private final CerrarSesionUseCase useCase;

    public CerrarSesionInteractor(final CerrarSesionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso CerrarSesionUseCase es obligatorio.");
    }

    @Override
    public void execute(final CerrarSesionDTO dto) {
        final CerrarSesionDomain domain = CerrarSesionMapper.toDomain(dto);
        useCase.execute(domain);
    }
}