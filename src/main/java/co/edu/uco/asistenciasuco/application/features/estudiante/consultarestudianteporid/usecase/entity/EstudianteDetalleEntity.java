package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudianteResumenEntity;

import java.util.List;

public record EstudianteDetalleEntity(
        EstudianteResumenEntity datosPersonales,
        List<EstudianteContextoAcademicoEntity> contextosAcademicos
) {
}
