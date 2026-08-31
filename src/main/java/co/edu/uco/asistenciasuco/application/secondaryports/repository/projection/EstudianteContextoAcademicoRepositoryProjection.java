package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

public record EstudianteContextoAcademicoRepositoryProjection(
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
