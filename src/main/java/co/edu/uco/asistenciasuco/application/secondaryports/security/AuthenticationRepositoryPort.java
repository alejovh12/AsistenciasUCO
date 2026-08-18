package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepositoryPort {

    Optional<UsuarioAutenticacionData> consultarPorCorreo(String correo);

    List<UsuarioPerfilData> consultarPerfilesPorUsuario(UUID idUsuario);
}
