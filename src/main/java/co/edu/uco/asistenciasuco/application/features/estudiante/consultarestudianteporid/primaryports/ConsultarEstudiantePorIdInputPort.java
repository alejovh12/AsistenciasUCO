package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

public interface ConsultarEstudiantePorIdInputPort extends InteractorWithReturn<UUID, EstudianteDetalleDTO> {
}
