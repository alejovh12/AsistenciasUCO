package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de usuarios.
 */
public final class UsuarioRepositoryMockAdapter implements UsuarioRepositoryPort {

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
        return Optional.empty();
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
        return Optional.empty();
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion
    ) {
        return Optional.empty();
    }

    @Override
    public java.util.Optional<String> consultarCorreoPorEstudiante(final java.util.UUID idEstudiante) {
        return java.util.Optional.empty();
    }
}
