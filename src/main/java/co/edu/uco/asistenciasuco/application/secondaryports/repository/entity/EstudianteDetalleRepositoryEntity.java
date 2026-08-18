package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.List;

public record EstudianteDetalleRepositoryEntity(
        EstudianteResumenRepositoryEntity datosPersonales,
        List<EstudianteContextoAcademicoRepositoryEntity> contextosAcademicos
) {
}
