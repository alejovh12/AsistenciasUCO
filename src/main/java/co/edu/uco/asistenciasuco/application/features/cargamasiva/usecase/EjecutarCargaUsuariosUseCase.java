package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;

public interface EjecutarCargaUsuariosUseCase {
    ResultadoCargaDTO execute(EjecutarCargaUsuariosCommand command);
}
