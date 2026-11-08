package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaResultado;

import java.util.List;
import java.util.UUID;

public record FilaResultadoCargaDTO(
        int numeroFila,
        String documento,
        String correo,
        EstadoFilaResultado estado,
        UUID idUsuario,
        List<ErrorFilaCargaDTO> errores
) {
    public FilaResultadoCargaDTO {
        errores = errores == null ? List.of() : List.copyOf(errores);
    }
}
