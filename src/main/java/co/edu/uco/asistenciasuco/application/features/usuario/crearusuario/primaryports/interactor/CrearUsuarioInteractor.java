package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.mapper.CrearUsuarioMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para crear usuario.
 */
public final class CrearUsuarioInteractor implements CrearUsuarioInputPort {

    private final CrearUsuarioUseCase useCase;

    public CrearUsuarioInteractor(final CrearUsuarioUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso CrearUsuarioUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public CrearUsuarioResultadoDTO execute(final CrearUsuarioDTO dto) {
        final CrearUsuarioDomain domain = CrearUsuarioMapper.toDomain(dto);
        final CrearUsuarioResultadoEntity resultado = useCase.execute(domain);
        return CrearUsuarioMapper.toDTO(resultado);
    }
}
