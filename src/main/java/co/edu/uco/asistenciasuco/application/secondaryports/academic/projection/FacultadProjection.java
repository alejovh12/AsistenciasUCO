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
        String estaActivaTextoFacultad,
        int totalProgramas,
        int totalEstudiantes
) {
    /** Sin totales (comandos y consultas puntuales que no los necesitan). */
    public FacultadProjection(final UUID id, final String nombreFacultad, final UUID idInstitucion,
                              final String nombreInstitucion, final UUID idDecano, final String nombreCompletoDecano,
                              final boolean estaActivaFacultad, final String estaActivaTextoFacultad) {
        this(id, nombreFacultad, idInstitucion, nombreInstitucion, idDecano, nombreCompletoDecano,
                estaActivaFacultad, estaActivaTextoFacultad, 0, 0);
    }
}
