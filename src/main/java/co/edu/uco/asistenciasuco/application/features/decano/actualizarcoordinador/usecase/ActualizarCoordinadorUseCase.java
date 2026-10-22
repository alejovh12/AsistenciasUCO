package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain.ActualizarCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;

public interface ActualizarCoordinadorUseCase {

    ResultadoGestionCoordinadorDomain execute(ActualizarCoordinadorDomain domain);
}
