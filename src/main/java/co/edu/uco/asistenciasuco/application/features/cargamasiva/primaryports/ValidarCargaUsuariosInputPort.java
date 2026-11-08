package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;

public interface ValidarCargaUsuariosInputPort {
    ResultadoValidacionCargaDTO validar(ValidarCargaUsuariosCommand command);
}
