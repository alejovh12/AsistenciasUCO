package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto;

import java.util.UUID;

public record DecanoDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                        UUID idFacultad, String nombreFacultad, boolean estaActivoDecano,
                        String primerNombre, String segundoNombre, String primerApellido, String segundoApellido,
                        String nombres, String apellidos) {

    /** Sin los nombres separados (respuestas que no los traen, p. ej. el cambio de estado). */
    public DecanoDTO(final UUID id, final UUID idUsuario, final String numeroIdentificacion, final String nombreCompleto,
                     final UUID idFacultad, final String nombreFacultad, final boolean estaActivoDecano) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano,
                null, null, null, null, null, null);
    }
}
