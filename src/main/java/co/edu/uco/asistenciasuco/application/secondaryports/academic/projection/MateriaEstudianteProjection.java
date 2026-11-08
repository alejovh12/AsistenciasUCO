package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record MateriaEstudianteProjection(
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idGrupo,
        String nombreGrupo,
        Integer creditos
) {

    /** Sin creditos (consultas que no los traen). */
    public MateriaEstudianteProjection(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo, final String nombreGrupo) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, null);
    }
}
