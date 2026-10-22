package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;

public interface ActualizarProgramaInputPort {

    ProgramaAcademicoDTO execute(ActualizarProgramaDTO dto);
}
