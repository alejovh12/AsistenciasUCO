package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface CoordinadorCommandPort {

    void crearCoordinador(UUID idCoordinador, String numeroIdentificacion, String primerNombre, String segundoNombre,
                          String primerApellido, String segundoApellido, String correo, UUID idPrograma,
                          UUID idFacultad, String password, UUID usuarioEjecutor);
}
