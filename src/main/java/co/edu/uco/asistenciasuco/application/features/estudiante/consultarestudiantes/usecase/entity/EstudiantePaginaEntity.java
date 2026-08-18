package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity;

import java.util.List;

public record EstudiantePaginaEntity(
        List<EstudianteResumenEntity> items,
        long totalItems,
        int totalPages,
        int page,
        int size
) {
}
