package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.UsuarioIdentidadRepositoryEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con usuarios.
 */
public interface UsuarioRepositoryPort {

    CrearUsuarioRepositoryEntity crearUsuario(CrearUsuarioRepositoryDTO dto);

    Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorCorreo(String correo);

    Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorIdentificacion(
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion
    );
}
