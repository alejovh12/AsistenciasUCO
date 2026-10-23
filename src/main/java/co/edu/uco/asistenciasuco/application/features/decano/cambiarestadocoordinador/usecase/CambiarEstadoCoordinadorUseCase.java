package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.domain.CambiarEstadoCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;

public interface CambiarEstadoCoordinadorUseCase {

    ResultadoGestionCoordinadorDomain execute(CambiarEstadoCoordinadorDomain domain);
}
