package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;

public interface ActualizarTipoProgramaInputPort {
    void execute(ActualizarTipoProgramaDTO dto);
}
