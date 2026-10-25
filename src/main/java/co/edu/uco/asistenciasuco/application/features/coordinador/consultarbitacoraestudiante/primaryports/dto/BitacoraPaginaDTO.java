package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto;

import java.util.List;

public record BitacoraPaginaDTO(List<BitacoraSesionDTO> sesiones, int totalRegistros) {
}
