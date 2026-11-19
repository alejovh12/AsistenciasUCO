package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto;

import java.util.UUID;

/**
 * HU074: salida del puerto primario al activar o inactivar una asignatura.
 *
 * @param idAsignatura          asignatura afectada.
 * @param estaActivaAsignatura  estado en que quedo la asignatura.
 * @param mensajeUsuario        texto institucional del catalogo, listo para mostrar.
 */
public record EstadoAsignaturaDTO(UUID idAsignatura, boolean estaActivaAsignatura, String mensajeUsuario) {
}
