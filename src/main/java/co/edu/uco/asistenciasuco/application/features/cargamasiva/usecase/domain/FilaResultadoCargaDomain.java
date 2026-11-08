package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.List;
import java.util.UUID;

public record FilaResultadoCargaDomain(
        int numeroFila,
        String numeroDocumento,
        String correo,
        EstadoFilaResultado estado,
        UUID idUsuarioCreado,
        List<ErrorFilaCargaDomain> errores
) {
}
