package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;

public interface EjecutarCargaUsuariosInputPort {
    ResultadoCargaDTO ejecutar(EjecutarCargaUsuariosCommand command);
}
