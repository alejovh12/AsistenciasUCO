package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.List;

public record ResultadoCargaDomain(
        int totalFilas,
        int creadas,
        int yaExistentes,
        int conError,
        int cuentasPendientes,
        List<FilaResultadoCargaDomain> filas,
        String mensaje
) {
}
