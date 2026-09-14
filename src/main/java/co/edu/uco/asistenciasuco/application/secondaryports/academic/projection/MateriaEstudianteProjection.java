package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record MateriaEstudianteProjection(
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idGrupo,
        String nombreGrupo
) {
}
