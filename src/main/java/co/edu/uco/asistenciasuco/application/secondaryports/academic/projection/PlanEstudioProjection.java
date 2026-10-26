package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** {@code totalSemestres} es el mayor numero de semestre del plan (0 si no tiene semestres; HU082/HU083). */
public record PlanEstudioProjection(
        UUID id,
        UUID idPrograma,
        String nombrePrograma,
        String inp,
        boolean estaActivoPlanEstudio,
        String estaActivoTextoPlanEstudio,
        String justificacionEstado,
        int totalSemestres
) {

    public PlanEstudioProjection(final UUID id, final UUID idPrograma, final String nombrePrograma, final String inp,
                                 final boolean estaActivoPlanEstudio, final String estaActivoTextoPlanEstudio,
                                 final String justificacionEstado) {
        this(id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio, estaActivoTextoPlanEstudio,
                justificacionEstado, 0);
    }
}
