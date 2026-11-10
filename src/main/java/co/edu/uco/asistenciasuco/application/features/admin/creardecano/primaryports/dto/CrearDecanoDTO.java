package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto;

import java.util.UUID;

public record CrearDecanoDTO(UUID tipoIdentificacionId, Integer numeroIdentificacion, String primerNombre, String segundoNombre,
                             String primerApellido, String segundoApellido, String correo,
                             String password, UUID idFacultad, UUID usuarioEjecutor,
                             FacultadNuevaDTO facultadNueva) {

    /** Alta sin facultad nueva: {@code idFacultad} opcional (comportamiento previo a DEC-065). */
    public CrearDecanoDTO(UUID tipoIdentificacionId, Integer numeroIdentificacion, String primerNombre, String segundoNombre,
                          String primerApellido, String segundoApellido, String correo,
                          String password, UUID idFacultad, UUID usuarioEjecutor) {
        this(tipoIdentificacionId, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido,
                correo, password, idFacultad, usuarioEjecutor, null);
    }
}
