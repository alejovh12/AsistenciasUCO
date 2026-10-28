package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

/** Pagina de la oferta de grupos del estudiante (HU008). */
public record OfertaGruposEstudiantePagina(List<GrupoOfertaEstudianteProjection> grupos, long total) {

    public OfertaGruposEstudiantePagina {
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }
}
