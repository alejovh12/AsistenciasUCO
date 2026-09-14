package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;

public interface CrearGrupoInputPort {

    CrearGrupoResultadoDTO execute(CrearGrupoDTO dto);
}
