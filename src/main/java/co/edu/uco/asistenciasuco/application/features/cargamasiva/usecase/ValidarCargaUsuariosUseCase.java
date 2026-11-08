package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ValidarCargaUsuariosDomain;

public interface ValidarCargaUsuariosUseCase {
    ResultadoValidacionCargaDomain execute(ValidarCargaUsuariosDomain domain);
}
