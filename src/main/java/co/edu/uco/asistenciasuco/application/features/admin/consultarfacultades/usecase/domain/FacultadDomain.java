package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de facultades.
 */
public record FacultadDomain(UUID id, String nombreFacultad, UUID idInstitucion, String nombreInstitucion,
                              UUID idDecano, String nombreCompletoDecano,
                              boolean estaActivaFacultad, String estaActivaTextoFacultad) {
}
