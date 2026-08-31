package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con usuarios.
 */
public interface UsuarioRepositoryPort {

    CrearUsuarioRepositoryProjection crearUsuario(CrearUsuarioRepositoryDTO dto);

    Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(String correo);

    Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion
    );
}
