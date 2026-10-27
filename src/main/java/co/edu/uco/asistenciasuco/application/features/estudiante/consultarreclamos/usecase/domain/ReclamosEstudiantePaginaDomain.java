package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain;

import java.util.List;

public record ReclamosEstudiantePaginaDomain(List<ReclamoEstudianteDomain> reclamos, int totalRegistros) {
}
