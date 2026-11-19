package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto;

/** HU095: totales del directorio docente del programa (sin desglose por tipo de vinculacion desde MEJORAS-SERGIO-2026-11-18). */
public record ResumenDocentesProgramaDTO(
        int totalDocentes,
        int totalActivos,
        int totalInactivos
) {
}
