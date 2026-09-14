package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;

public interface ActualizarSesionInputPort {

    void execute(ActualizarSesionDTO dto);
}
