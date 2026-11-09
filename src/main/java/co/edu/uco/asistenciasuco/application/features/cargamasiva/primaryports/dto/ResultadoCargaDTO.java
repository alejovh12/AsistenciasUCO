package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import java.util.List;

public record ResultadoCargaDTO(
        int totalFilas,
        int creadas,
        int yaExistentes,
        int conError,
        int cuentasPendientes,
        List<FilaResultadoCargaDTO> filas,
        String mensaje
) {
    public ResultadoCargaDTO {
        filas = filas == null ? List.of() : List.copyOf(filas);
    }
}
