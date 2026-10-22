package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Resultado de editar o cambiar el estado de un coordinador, con lo necesario para sincronizar su cuenta de identidad.
 *
 * @param idUsuario              usuario del coordinador.
 * @param personaSinRolesActivos {@code true} si, tras desactivar el rol, la persona no conserva ningun otro rol
 *                               activo (la cuenta de identidad debe deshabilitarse). Siempre {@code false} al editar
 *                               o reactivar.
 */
public record ResultadoGestionCoordinadorProjection(UUID idUsuario, boolean personaSinRolesActivos) {
}
