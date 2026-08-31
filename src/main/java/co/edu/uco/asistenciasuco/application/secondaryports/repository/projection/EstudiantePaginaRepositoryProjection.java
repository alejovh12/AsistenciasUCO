package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.List;

public record EstudiantePaginaRepositoryProjection(
        List<EstudianteResumenRepositoryProjection> items,
        long totalItems,
        int totalPages,
        int page,
        int size
) {
}
