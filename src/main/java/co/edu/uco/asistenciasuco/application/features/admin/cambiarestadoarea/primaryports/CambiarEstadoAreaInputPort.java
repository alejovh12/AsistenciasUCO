package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;

public interface CambiarEstadoAreaInputPort {
    void execute(CambiarEstadoAreaDTO dto);
}
