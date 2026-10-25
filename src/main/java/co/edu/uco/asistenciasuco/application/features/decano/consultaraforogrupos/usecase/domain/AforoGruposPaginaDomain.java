package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain;

import java.util.List;

public record AforoGruposPaginaDomain(List<AforoGrupoDomain> grupos, int totalRegistros) {
}
