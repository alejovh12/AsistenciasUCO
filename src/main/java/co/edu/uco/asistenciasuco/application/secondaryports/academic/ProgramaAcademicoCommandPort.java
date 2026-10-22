package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

/** Comandos de persistencia del programa academico (HU076, HU077, HU078). */
public interface ProgramaAcademicoCommandPort {

    void crear(CrearProgramaCommand command);

    void actualizar(ActualizarProgramaCommand command);

    void cambiarEstado(CambiarEstadoProgramaCommand command);

    record CrearProgramaCommand(
            UUID id,
            UUID idFacultad,
            String nombre,
            UUID idTipoPrograma,
            UUID idCoordinador,
            UUID idModalidad,
            UUID idJornada,
            UUID actorUsuarioId
    ) {
    }

    record ActualizarProgramaCommand(
            UUID id,
            String nombre,
            UUID idTipoPrograma,
            UUID idCoordinador,
            UUID idModalidad,
            UUID idJornada,
            UUID actorUsuarioId
    ) {
    }

    record CambiarEstadoProgramaCommand(UUID id, boolean estaActivo, UUID actorUsuarioId) {
    }
}
