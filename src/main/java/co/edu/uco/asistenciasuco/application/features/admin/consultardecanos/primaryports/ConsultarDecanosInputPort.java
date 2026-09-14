package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;

import java.util.List;

public interface ConsultarDecanosInputPort {
    List<DecanoDTO> execute();
}
