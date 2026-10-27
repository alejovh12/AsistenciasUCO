package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;

public interface ConsultarKpisCoordinadorInputPort {

    KpisCoordinadorDTO execute(ConsultarKpisCoordinadorDTO dto);
}
