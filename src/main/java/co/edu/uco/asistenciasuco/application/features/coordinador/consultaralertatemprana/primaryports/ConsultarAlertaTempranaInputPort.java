package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;

public interface ConsultarAlertaTempranaInputPort {

    AlertasPaginaDTO execute(ConsultarAlertaTempranaDTO dto);
}
