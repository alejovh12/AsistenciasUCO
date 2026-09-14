package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record FacultadProjection(
        UUID id,
        String nombreFacultad,
        UUID idInstitucion,
        String nombreInstitucion,
        UUID idDecano,
        String nombreCompletoDecano,
        boolean estaActivaFacultad,
        String estaActivaTextoFacultad
) {
}
