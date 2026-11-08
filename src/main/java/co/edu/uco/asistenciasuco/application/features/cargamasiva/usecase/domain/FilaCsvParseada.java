package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

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
        String codigoTipoVinculacion,
        String codigoPrograma,
        String codigoFacultad,
        String claveInicial,
        List<ErrorFilaCargaDomain> erroresLocales
) {
}
