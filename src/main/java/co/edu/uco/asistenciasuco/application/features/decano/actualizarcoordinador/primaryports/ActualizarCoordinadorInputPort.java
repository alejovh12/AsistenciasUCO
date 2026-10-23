package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;

public interface ActualizarCoordinadorInputPort {

    ResultadoGestionCoordinadorDTO execute(ActualizarCoordinadorDTO dto);
}
