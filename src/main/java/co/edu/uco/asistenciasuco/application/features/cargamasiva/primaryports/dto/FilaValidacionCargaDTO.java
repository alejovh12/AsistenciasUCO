package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaValidacion;

import java.util.List;

public record FilaValidacionCargaDTO(
        int numeroFila,
        String documento,
        String correo,
        EstadoFilaValidacion estado,
        List<ErrorFilaCargaDTO> errores
) {
    public FilaValidacionCargaDTO {
        errores = errores == null ? List.of() : List.copyOf(errores);
    }
}
