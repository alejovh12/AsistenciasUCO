package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain;

import java.util.List;

public record OfertaGruposPaginaDomain(List<GrupoOfertaDomain> grupos, long total) {

    public OfertaGruposPaginaDomain {
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }
}
