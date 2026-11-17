package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record DecanoProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        UUID idFacultad,
        String nombreFacultad,
        boolean estaActivoDecano,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String tipoIdentificacion
) {

    /** Sin correo ni tipo de documento (consultas que no los traen). */
    public DecanoProjection(final UUID id, final UUID idUsuario, final String numeroIdentificacion,
                            final String nombreCompleto, final UUID idFacultad, final String nombreFacultad,
                            final boolean estaActivoDecano, final String primerNombre, final String segundoNombre,
                            final String primerApellido, final String segundoApellido) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano,
                primerNombre, segundoNombre, primerApellido, segundoApellido, null, null);
    }

    /** CIERRE-W1B: sin los nombres separados (consultas que no los necesitan). */
    public DecanoProjection(final UUID id, final UUID idUsuario, final String numeroIdentificacion,
                            final String nombreCompleto, final UUID idFacultad, final String nombreFacultad,
                            final boolean estaActivoDecano) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano,
                null, null, null, null, null, null);
    }
}
