package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

/**
 * Datos de una cuenta de identidad que se pueden actualizar de forma neutral. No incluye el nombre de usuario
 * (es el documento y no se edita) ni la contrasena (no existen flujos de cambio en este programa).
 *
 * @param correo         correo electronico vigente del usuario.
 * @param primerNombre   primer nombre.
 * @param primerApellido primer apellido.
 */
public record ActualizarCuentaIdentidadDTO(
        String correo,
        String primerNombre,
        String primerApellido
) {
}
