package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

/**
 * Resultado de la creación de una cuenta en el proveedor de identidad.
 * El campo {@code idExterno} es el identificador asignado por el IdP
 * (UUID de Keycloak, sub de Auth0, etc.) — útil para operaciones de compensación.
 *
 * @param idExterno ID único asignado por el IdP al usuario recién creado.
 * @param username  Username con el que fue registrado en el IdP.
 * @param mensaje   Mensaje descriptivo del resultado de la operación.
 */
public record CuentaIdentidadDTO(
        String idExterno,
        String username,
        String mensaje
) {
}
