package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto;

import java.util.List;

public record AforoGruposPaginaDTO(List<AforoGrupoDTO> grupos, int totalRegistros) {
}
