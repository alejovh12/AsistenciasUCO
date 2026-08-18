package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity;

import java.util.UUID;

public record EstudianteContextoAcademicoEntity(
        UUID idInstitucion,
        String nombreInstitucion,
        UUID idFacultad,
        String nombreFacultad,
        UUID idPrograma,
        String nombrePrograma,
        UUID idPlanEstudio,
        String inpPlanEstudio,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idGrupo,
        String nombreGrupo
) {
}
