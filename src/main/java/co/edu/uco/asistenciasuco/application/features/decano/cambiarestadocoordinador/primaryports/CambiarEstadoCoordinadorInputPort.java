package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;

public interface CambiarEstadoCoordinadorInputPort {

    ResultadoGestionCoordinadorDTO execute(CambiarEstadoCoordinadorDTO dto);
}
