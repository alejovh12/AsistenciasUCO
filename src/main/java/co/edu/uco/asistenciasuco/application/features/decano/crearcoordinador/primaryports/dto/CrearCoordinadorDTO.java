package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto;

import java.util.UUID;

public record CrearCoordinadorDTO(
        UUID tipoIdentificacionId,
        String numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID idPrograma,
        String password,
        UUID usuario,
        ProgramaNuevoDTO programaNuevo
) {
    /** Alta sin programa nuevo: {@code idPrograma} opcional (comportamiento previo a DEC-065). */
    public CrearCoordinadorDTO(
            UUID tipoIdentificacionId,
            String numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            UUID idPrograma,
            String password,
            UUID usuario
    ) {
        this(tipoIdentificacionId, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido, correo,
                idPrograma, password, usuario, null);
    }

    public CrearCoordinadorDTO(
            String numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            UUID idPrograma,
            String password,
            UUID usuario
    ) {
        this(null, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido, correo, idPrograma, password, usuario);
    }
}
