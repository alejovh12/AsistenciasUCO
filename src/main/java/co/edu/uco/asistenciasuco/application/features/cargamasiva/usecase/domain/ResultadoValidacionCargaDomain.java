package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.List;

public record ResultadoValidacionCargaDomain(
        String huella,
        PerfilDestinoCarga perfilDestino,
        int totalFilas,
        int validas,
        int yaExistentes,
        int conError,
        List<FilaValidacionCargaDomain> filas
) {
}
