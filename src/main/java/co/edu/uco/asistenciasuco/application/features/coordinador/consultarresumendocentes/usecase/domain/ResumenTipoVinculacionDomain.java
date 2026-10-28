package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain;

import java.util.UUID;

public record ResumenTipoVinculacionDomain(
        UUID idTipoVinculacion,
        String codigoTipoVinculacion,
        String nombreTipoVinculacion,
        int totalActivos,
        int totalInactivos
) {

    public int total() {
        return totalActivos + totalInactivos;
    }
}
