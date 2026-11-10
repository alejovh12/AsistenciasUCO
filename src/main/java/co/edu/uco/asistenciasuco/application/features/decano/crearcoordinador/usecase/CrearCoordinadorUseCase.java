package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorResultado;

public interface CrearCoordinadorUseCase {

    CrearCoordinadorResultado execute(CrearCoordinadorDomain domain);
}
