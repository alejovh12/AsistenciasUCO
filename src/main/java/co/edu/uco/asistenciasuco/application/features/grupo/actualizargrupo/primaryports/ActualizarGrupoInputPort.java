package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;

public interface ActualizarGrupoInputPort {

    ActualizarGrupoResultadoDTO execute(ActualizarGrupoDTO dto);
}
