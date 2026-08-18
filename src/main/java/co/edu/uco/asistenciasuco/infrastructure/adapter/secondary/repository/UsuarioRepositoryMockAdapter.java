package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.UsuarioIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de usuarios.
 */
public final class UsuarioRepositoryMockAdapter implements UsuarioRepositoryPort {

    @Override
    public CrearUsuarioRepositoryEntity crearUsuario(final CrearUsuarioRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        return new CrearUsuarioRepositoryEntity("Usuario registrado exitosamente.");
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorCorreo(final String correo) {
        return Optional.empty();
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorIdentificacion(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion
    ) {
        return Optional.empty();
    }
}
