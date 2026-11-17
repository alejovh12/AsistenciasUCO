package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de decanos.
 */
public record DecanoDomain(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                            UUID idFacultad, String nombreFacultad, boolean estaActivoDecano,
                            String primerNombre, String segundoNombre, String primerApellido, String segundoApellido,
                            String correo, String tipoIdentificacion) {

    /** Sin los nombres separados. */
    public DecanoDomain(final UUID id, final UUID idUsuario, final String numeroIdentificacion,
                        final String nombreCompleto, final UUID idFacultad, final String nombreFacultad,
                        final boolean estaActivoDecano) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano,
                null, null, null, null, null, null);
    }
}
