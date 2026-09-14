package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;

public interface RegistrarAsistenciasSesionInputPort {

    void execute(RegistrarAsistenciasSesionDTO dto);
}
