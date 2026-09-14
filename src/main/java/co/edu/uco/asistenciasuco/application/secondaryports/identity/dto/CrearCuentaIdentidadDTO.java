package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.UUID;

/**
 * Datos necesarios para crear una cuenta institucional en el proveedor de identidad.
 * Este DTO es agnóstico al IdP y no contiene referencias a tecnologías concretas.
 *
 * @param username          Nombre de usuario institucional (ej: número de identificación).
 * @param idUsuario         Identificador interno del usuario en la DB institucional.
 * @param correo            Correo electrónico institucional.
 * @param primerNombre      Primer nombre del usuario.
 * @param primerApellido    Primer apellido del usuario.
 * @param passwordInicial   Contraseña inicial asignada por el sistema. Solo se aplica si el
 *                          IdP determina que la cuenta es nueva; nunca se usa para resetear
 *                          la contraseña de una cuenta ya existente.
 * @param rolInstitucional  Rol institucional a asignar en el IdP. El adapter traduce este enum
 *                          al nombre exacto del client role en el IdP (ej. {@code name()}) —
 *                          Application nunca envía nombres técnicos, alias, ni prefijos como
 *                          {@code ROLE_}.
 */
public record CrearCuentaIdentidadDTO(
        String username,
        UUID idUsuario,
        String correo,
        String primerNombre,
        String primerApellido,
        String passwordInicial,
        InstitutionalRole rolInstitucional
) {
}
