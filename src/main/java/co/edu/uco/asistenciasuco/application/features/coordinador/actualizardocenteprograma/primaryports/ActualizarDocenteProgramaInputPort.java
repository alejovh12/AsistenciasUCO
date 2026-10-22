package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto.ActualizarDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;

public interface ActualizarDocenteProgramaInputPort {

    DocenteProgramaDTO execute(ActualizarDocenteProgramaDTO dto);
}
