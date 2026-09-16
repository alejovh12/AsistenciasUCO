package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface DecanoCommandPort {

    void crearDecano(CrearDecanoCommand command);

    record CrearDecanoCommand(
            UUID idDecano,
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            UUID idFacultad,
            String nombreFacultad,
            String password,
            UUID usuarioEjecutor
    ) {
    }
}
