package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record InstitucionProjection(
        UUID id,
        String nombre,
        boolean estaActivaInstitucion,
        String estaActivaTextoInstitucion
) {
}
