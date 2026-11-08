package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;

public interface ValidarCargaUsuariosUseCase {
    ResultadoValidacionCargaDTO execute(ValidarCargaUsuariosCommand command);
}
