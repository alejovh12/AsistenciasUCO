package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record CoordinadorProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        UUID idPrograma,
        String nombrePrograma,
        boolean estaActivoCoordinador,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        boolean estaActivoRolCoordinador
) {

    /** Proyeccion sin los datos editables (consultas que no los necesitan). */
    public CoordinadorProjection(
            final UUID id,
            final UUID idUsuario,
            final String numeroIdentificacion,
            final String nombreCompleto,
            final UUID idPrograma,
            final String nombrePrograma,
            final boolean estaActivoCoordinador
    ) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma, nombrePrograma, estaActivoCoordinador,
                null, null, null, null, null, estaActivoCoordinador);
    }
}
