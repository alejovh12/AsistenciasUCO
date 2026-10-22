package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;

public interface CambiarEstadoProgramaInputPort {

    ProgramaAcademicoDTO execute(CambiarEstadoProgramaDTO dto);
}
