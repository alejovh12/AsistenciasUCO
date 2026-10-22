package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response;

import java.util.UUID;

public record ProgramaAcademicoResponse(
        UUID id,
        String nombre,
        UUID idTipoPrograma,
        String nombreTipoPrograma,
        UUID idModalidad,
        String nombreModalidad,
        UUID idJornada,
        String nombreJornada,
        UUID idCoordinador,
        String nombreCoordinador,
        UUID idFacultad,
        String nombreFacultad,
        boolean estaActivo
) {
}
