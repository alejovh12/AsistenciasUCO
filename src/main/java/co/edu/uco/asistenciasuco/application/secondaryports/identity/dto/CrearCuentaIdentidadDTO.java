package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

/**
 * Datos necesarios para crear una cuenta institucional en el proveedor de identidad.
 * Este DTO es agnóstico al IdP — no contiene ninguna referencia a Keycloak, Auth0 u otro.
 *
 * @param username          Nombre de usuario institucional (ej: número de identificación).
 * @param correo            Correo electrónico institucional.
 * @param primerNombre      Primer nombre del usuario.
 * @param primerApellido    Primer apellido del usuario.
 * @param passwordInicial   Contraseña inicial asignada por el sistema.
 * @param rolInstitucional  Rol a asignar en el IdP (ej: "docente", "coordinador", "decano").
 */
public record CrearCuentaIdentidadDTO(
        String username,
        String correo,
        String primerNombre,
        String primerApellido,
        String passwordInicial,
        String rolInstitucional
) {
}
