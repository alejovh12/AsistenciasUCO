package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

public interface ConsultarEstudiantesInputPort extends InteractorWithReturn<ConsultarEstudiantesDTO, EstudiantePaginaDTO> {
}
