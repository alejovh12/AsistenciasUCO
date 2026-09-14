package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.ProvisionarUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.mapper.ProvisionarUsuarioMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.ProvisionarUsuarioUseCase;

import java.util.Objects;

public final class ProvisionarUsuarioInteractor implements ProvisionarUsuarioInputPort {

    private final ProvisionarUsuarioUseCase useCase;

    public ProvisionarUsuarioInteractor(final ProvisionarUsuarioUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ProvisionarUsuarioUseCase es obligatorio.");
    }

    @Override
    public ProvisionarUsuarioResultadoDTO execute(final ProvisionarUsuarioDTO dto) {
        return ProvisionarUsuarioMapper.toDTO(useCase.execute(ProvisionarUsuarioMapper.toDomain(dto)));
    }
}
