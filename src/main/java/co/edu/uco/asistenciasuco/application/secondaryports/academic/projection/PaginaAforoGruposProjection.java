package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

public record PaginaAforoGruposProjection(List<AforoGrupoProjection> grupos, int totalRegistros) {

    public PaginaAforoGruposProjection {
        grupos = List.copyOf(grupos);
    }
}
