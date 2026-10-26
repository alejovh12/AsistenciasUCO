package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto;

/**
 * Situación de un rol institucional de la cuenta en cada capa.
 *
 * @param rol         rol institucional.
 * @param esperado    la base institucional lo mantiene activo.
 * @param enProveedor el proveedor de identidad lo tiene asignado.
 * @param perfil      estado del perfil equivalente en el motor de autorización.
 */
public record AccesoRolDTO(String rol, boolean esperado, boolean enProveedor, String perfil) {
}
