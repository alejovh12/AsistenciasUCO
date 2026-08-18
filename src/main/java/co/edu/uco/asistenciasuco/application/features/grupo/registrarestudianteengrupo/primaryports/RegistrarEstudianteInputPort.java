package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para registrar un estudiante en un grupo.
 */
public interface RegistrarEstudianteInputPort
        extends InteractorWithReturn<RegistrarEstudianteDTO, RegistrarEstudianteResultadoDTO> {
}
