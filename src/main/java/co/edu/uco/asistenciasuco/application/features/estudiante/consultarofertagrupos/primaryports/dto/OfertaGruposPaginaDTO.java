package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto;

import java.util.List;

public record OfertaGruposPaginaDTO(List<GrupoOfertaDTO> grupos, long total) {
}
