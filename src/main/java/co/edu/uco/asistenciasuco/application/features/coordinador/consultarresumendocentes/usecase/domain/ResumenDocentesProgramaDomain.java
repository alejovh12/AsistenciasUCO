package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain;

public record ResumenDocentesProgramaDomain(
        int totalDocentes,
        int totalActivos,
        int totalInactivos
) {
}
