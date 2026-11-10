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
     * DEC-065 (5): registra, en una sola transaccion de BD, a la persona coordinadora y el programa academico que
     * coordinara en la facultad del decano ({@code usp_crear_coordinador_con_programa}). Ante cualquier rechazo no
     * queda persona ni programa.
     */
    ResultadoCoordinadorConPrograma crearCoordinadorConPrograma(CrearCoordinadorConProgramaCommand command);

    /** Identificadores y nombre confirmados por la base de datos para el alta compuesta. */
    record ResultadoCoordinadorConPrograma(UUID idCoordinador, UUID idPrograma, String nombrePrograma) {
    }

    record CrearCoordinadorConProgramaCommand(
            UUID idCoordinador,
            UUID tipoIdentificacionId,
            String numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            String password,
            String nombrePrograma,
            UUID idTipoPrograma,
            UUID idModalidad,
            UUID idJornada,
            UUID idFacultad,
            UUID usuarioEjecutor
    ) {
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
