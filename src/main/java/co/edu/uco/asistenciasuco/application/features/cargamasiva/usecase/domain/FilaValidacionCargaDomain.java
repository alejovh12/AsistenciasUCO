package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.List;

public record FilaValidacionCargaDomain(
        int numeroFila,
        String numeroDocumento,
        String correo,
        EstadoFilaValidacion estado,
        List<ErrorFilaCargaDomain> errores
) {
}
