package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record PlanEstudioProjection(
        UUID id,
        UUID idPrograma,
        String nombrePrograma,
        String inp,
        boolean estaActivoPlanEstudio,
        String estaActivoTextoPlanEstudio,
        String justificacionEstado
) {
}
