package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto;

import java.util.UUID;

public record ResumenTipoVinculacionDTO(
        UUID idTipoVinculacion,
        String codigoTipoVinculacion,
        String nombreTipoVinculacion,
        int totalActivos,
        int totalInactivos,
        int total
) {
}
