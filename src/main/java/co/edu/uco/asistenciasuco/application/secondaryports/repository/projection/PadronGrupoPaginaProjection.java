package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.List;

/** Pagina del padron de un grupo: filas de la pagina y total de matriculas que cumplen el filtro (CIERRE-W1A). */
public record PadronGrupoPaginaProjection(List<EstudianteGrupoRepositoryProjection> estudiantes, long totalRegistros) {

    public PadronGrupoPaginaProjection {
        estudiantes = estudiantes == null ? List.of() : List.copyOf(estudiantes);
    }
}
