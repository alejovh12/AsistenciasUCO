package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para provisionar un usuario (creacion en la DB institucional y en el proveedor de identidad).
 */
public record ProvisionarUsuarioDTO(
        UUID tipoIdIdentificacion,
        Integer numeroIdentificacion,
        String primerApellido,
        String segundoApellido,
        String primerNombre,
        String segundoNombre,
        String correo,
        String password
) {
}
