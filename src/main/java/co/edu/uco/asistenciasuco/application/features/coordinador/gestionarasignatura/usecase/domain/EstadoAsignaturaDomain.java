package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain;

import java.util.UUID;

/**
 * HU074: resultado de activar o inactivar una asignatura.
 *
 * @param idAsignatura          asignatura afectada.
 * @param estaActivaAsignatura  estado en que quedo la asignatura.
 * @param mensajeUsuario        texto institucional del catalogo (ASI_011 quedo activa / ASI_012 quedo inactiva).
 */
public record EstadoAsignaturaDomain(UUID idAsignatura, boolean estaActivaAsignatura, String mensajeUsuario) {
}
