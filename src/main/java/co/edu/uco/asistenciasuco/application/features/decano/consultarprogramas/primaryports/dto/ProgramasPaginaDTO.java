package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import java.util.List;

public record ProgramasPaginaDTO(List<ProgramaAcademicoDTO> programas, int totalRegistros) {
}
