package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto;

import java.util.List;

public record EstudiantePaginaDTO(
        List<EstudianteResumenDTO> items,
        long totalItems,
        int totalPages,
        int page,
        int size
) {
}
