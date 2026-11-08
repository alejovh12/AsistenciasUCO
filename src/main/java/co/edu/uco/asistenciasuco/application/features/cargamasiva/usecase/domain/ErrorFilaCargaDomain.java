package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

public record ErrorFilaCargaDomain(
        String campo,
        String codigo,
        String mensaje
) {
    public String codigoError() {
        return codigo;
    }
}
