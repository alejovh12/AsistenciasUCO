package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.dto.CambiarEstadoDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;

public interface CambiarEstadoDocenteProgramaInputPort {

    DocenteProgramaDTO execute(CambiarEstadoDocenteProgramaDTO dto);
}
