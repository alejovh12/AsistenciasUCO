package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorResultadoDTO;

public interface CrearCoordinadorInputPort {

    CrearCoordinadorResultadoDTO execute(CrearCoordinadorDTO dto);
}
