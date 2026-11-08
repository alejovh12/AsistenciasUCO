package co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto;

public record FilaCsvParaSpDTO(
        int numeroFila,
        String tipoDocumento,
        String numeroDocumento,
        String correo,
        String codigoTipoVinculacion,
        String codigoPrograma,
        String codigoFacultad
) {
}
