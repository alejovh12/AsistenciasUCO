package co.edu.uco.asistenciasuco.application.features.decano.common.domain;

import java.util.UUID;

public record ProgramaAcademicoDomain(
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
