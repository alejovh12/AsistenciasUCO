package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;

public interface CambiarEstadoTipoProgramaInputPort {
    void execute(CambiarEstadoTipoProgramaDTO dto);
}
