package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto;

import java.util.UUID;

public record FacultadDTO(UUID id, String nombreFacultad, UUID idInstitucion, String nombreInstitucion,
                          UUID idDecano, String nombreCompletoDecano,
                          boolean estaActivaFacultad, String estaActivaTextoFacultad) {
}
