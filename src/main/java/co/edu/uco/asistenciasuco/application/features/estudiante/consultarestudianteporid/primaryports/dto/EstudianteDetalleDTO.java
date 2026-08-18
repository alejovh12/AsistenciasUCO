package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudianteResumenDTO;

import java.util.List;

public record EstudianteDetalleDTO(
        EstudianteResumenDTO datosPersonales,
        List<EstudianteContextoAcademicoDTO> contextosAcademicos
) {
}
