package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.List;

public record EstudianteDetalleRepositoryProjection(
        EstudianteResumenRepositoryProjection datosPersonales,
        List<EstudianteContextoAcademicoRepositoryProjection> contextosAcademicos
) {
}
