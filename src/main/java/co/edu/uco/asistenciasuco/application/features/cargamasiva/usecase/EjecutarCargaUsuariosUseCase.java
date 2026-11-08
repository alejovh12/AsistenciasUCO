package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;

public interface EjecutarCargaUsuariosUseCase {
    ResultadoCargaDomain execute(EjecutarCargaUsuariosDomain domain);
}
