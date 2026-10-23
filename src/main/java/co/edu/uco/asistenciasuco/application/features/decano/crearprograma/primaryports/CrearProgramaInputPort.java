package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;

public interface CrearProgramaInputPort {

    ProgramaAcademicoDTO execute(CrearProgramaDTO dto);
}
