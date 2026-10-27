package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto;

import java.util.List;

public record AlertasPaginaDTO(List<AlertaTempranaDTO> alertas, int totalRegistros) {
}
