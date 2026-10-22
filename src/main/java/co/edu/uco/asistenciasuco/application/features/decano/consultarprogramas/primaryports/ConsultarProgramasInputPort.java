package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;

public interface ConsultarProgramasInputPort {

    ProgramasPaginaDTO execute(ConsultarProgramasDTO dto);
}
