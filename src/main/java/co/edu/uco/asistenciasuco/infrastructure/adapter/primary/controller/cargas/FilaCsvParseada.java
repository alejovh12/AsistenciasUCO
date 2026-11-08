package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ErrorFilaCargaDTO;

import java.util.List;

public record FilaCsvParseada(
        int numeroFila,
        String tipoDocumento,
        String numeroDocumento,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String claveInicial,
        String codigoTipoVinculacion,
        String codigoPrograma,
        String codigoFacultad,
        List<ErrorFilaCargaDTO> erroresLocales
) {
    public FilaCsvParseada {
        erroresLocales = erroresLocales == null ? List.of() : List.copyOf(erroresLocales);
    }
}
