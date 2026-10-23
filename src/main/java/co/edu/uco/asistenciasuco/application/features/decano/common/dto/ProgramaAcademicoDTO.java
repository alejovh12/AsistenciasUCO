package co.edu.uco.asistenciasuco.application.features.decano.common.dto;

import java.util.UUID;

public record ProgramaAcademicoDTO(
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
