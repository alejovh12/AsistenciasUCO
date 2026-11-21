package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record EstudianteProgramaProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        String correo,
        UUID idPrograma,
        String nombrePrograma,
        Integer semestreActual,
        Boolean estaActivoUsuario
) {
    /** Compatibilidad: sin indicador de estado activo (consulta no paginada). */
    public EstudianteProgramaProjection(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto, String correo,
              UUID idPrograma, String nombrePrograma, Integer semestreActual) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, correo, idPrograma, nombrePrograma, semestreActual, null);
    }
}
