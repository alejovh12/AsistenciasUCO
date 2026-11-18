package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de facultades.
 */
public record FacultadDomain(UUID id, String nombreFacultad, UUID idInstitucion, String nombreInstitucion,
                              UUID idDecano, String nombreCompletoDecano,
                              boolean estaActivaFacultad, String estaActivaTextoFacultad,
                              int totalProgramas, int totalEstudiantes) {
    public FacultadDomain(final UUID id, final String nombreFacultad, final UUID idInstitucion,
                          final String nombreInstitucion, final UUID idDecano, final String nombreCompletoDecano,
                          final boolean estaActivaFacultad, final String estaActivaTextoFacultad) {
        this(id, nombreFacultad, idInstitucion, nombreInstitucion, idDecano, nombreCompletoDecano,
                estaActivaFacultad, estaActivaTextoFacultad, 0, 0);
    }
}
