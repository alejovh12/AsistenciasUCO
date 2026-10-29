package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto;

import java.util.List;
import java.util.UUID;

/** Perfil propio (HU006): datos personales de la DB y vinculos institucionales. Sin contrasena. */
public record PerfilUsuarioDTO(
        UUID idUsuario,
        String codigoTipoIdentificacion,
        String nombreTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        boolean correoConfirmado,
        boolean estaActivo,
        List<VinculoUsuarioDTO> vinculos
) {
}
