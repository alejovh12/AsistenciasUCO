package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.List;

public record EstudiantePaginaRepositoryEntity(
        List<EstudianteResumenRepositoryEntity> items,
        long totalItems,
        int totalPages,
        int page,
        int size
) {
}
