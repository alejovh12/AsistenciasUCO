package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;

import java.util.UUID;

public interface CoordinadorCommandPort {

    void crearCoordinador(UUID idCoordinador, String numeroIdentificacion, String primerNombre, String segundoNombre,
                          String primerApellido, String segundoApellido, String correo, UUID idPrograma,
                          UUID idFacultad, String password, UUID usuarioEjecutor, UUID idTipoIdIdentificacion);

    default void crearCoordinador(UUID idCoordinador, String numeroIdentificacion, String primerNombre, String segundoNombre,
                                  String primerApellido, String segundoApellido, String correo, UUID idPrograma,
                                  UUID idFacultad, String password, UUID usuarioEjecutor) {
        crearCoordinador(idCoordinador, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido,
                correo, idPrograma, idFacultad, password, usuarioEjecutor, null);
    }

    /**
     * HU121: edita nombres, apellidos y correo; HU159: corrige el documento si {@code documento} lo pide. Devuelve el
     * usuario del coordinador para sincronizar la identidad.
     */
    ResultadoGestionCoordinadorProjection actualizarCoordinador(UUID idCoordinador, String primerNombre,
                                                                String segundoNombre, String primerApellido,
                                                                String segundoApellido, String correo,
                                                                UUID idFacultad, UUID usuarioEjecutor,
                                                                CorreccionDocumentoDTO documento);

    /** HU122: activa o desactiva el rol de coordinador. Idempotente. */
    ResultadoGestionCoordinadorProjection cambiarEstadoCoordinador(UUID idCoordinador, boolean estaActivo,
                                                                   UUID idFacultad, UUID usuarioEjecutor);
}
