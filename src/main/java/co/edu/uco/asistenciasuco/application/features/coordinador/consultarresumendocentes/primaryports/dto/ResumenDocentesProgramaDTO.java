package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto;

import java.util.List;

public record ResumenDocentesProgramaDTO(
        int totalDocentes,
        int totalActivos,
        int totalInactivos,
        List<ResumenTipoVinculacionDTO> porTipoVinculacion
) {
}
