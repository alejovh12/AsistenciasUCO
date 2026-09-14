package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de instituciones.
 */
public record InstitucionDomain(UUID id, String nombre, boolean estaActivaInstitucion, String estaActivaTextoInstitucion) {
}
