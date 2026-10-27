package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto;

import java.util.List;

public record ReclamosEstudiantePaginaDTO(List<ReclamoEstudianteDTO> reclamos, int totalRegistros) {
}
