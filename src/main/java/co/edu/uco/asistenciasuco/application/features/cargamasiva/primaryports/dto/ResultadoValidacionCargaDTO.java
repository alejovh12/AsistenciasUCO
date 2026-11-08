package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;

import java.util.List;

public record ResultadoValidacionCargaDTO(
        String huella,
        PerfilDestinoCarga perfilDestino,
        int totalFilas,
        int filasValidas,
        int filasYaExistentes,
        int filasConError,
        List<FilaValidacionCargaDTO> filas
) {
    public ResultadoValidacionCargaDTO {
        filas = filas == null ? List.of() : List.copyOf(filas);
    }
}
