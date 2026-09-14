package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;

public interface RegistrarAsistenciaAutonomaInputPort {

    void execute(RegistrarAsistenciaAutonomaDTO dto);
}
