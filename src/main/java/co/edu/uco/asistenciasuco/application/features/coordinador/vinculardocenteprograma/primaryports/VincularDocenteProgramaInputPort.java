package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;

public interface VincularDocenteProgramaInputPort {

    DocenteProgramaDTO execute(VincularDocenteProgramaDTO dto);
}
