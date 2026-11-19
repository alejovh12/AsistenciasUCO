package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/**
 * HU074: lo que entrego la DB al activar o inactivar una asignatura.
 *
 * @param mensajeUsuario        texto del catalogo que entrego el SP (ASI_011 / ASI_012); puede venir vacio.
 * @param estaActivaResultante  estado en que quedo la asignatura segun el codigo de exito del SP (ASI_011 activa, ASI_012
 *                              inactiva); {@code null} si el SP no publico ese codigo.
 */
public record ResultadoEstadoAsignaturaProjection(String mensajeUsuario, Boolean estaActivaResultante) {
}
