package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response.ProgramaAcademicoResponse;

public final class ProgramaAcademicoHttpMapper {

    private ProgramaAcademicoHttpMapper() {
    }

    public static ProgramaAcademicoResponse toResponse(final ProgramaAcademicoDTO dto) {
        return new ProgramaAcademicoResponse(dto.id(), dto.nombre(), dto.idTipoPrograma(), dto.nombreTipoPrograma(),
                dto.idModalidad(), dto.nombreModalidad(), dto.idJornada(), dto.nombreJornada(), dto.idCoordinador(),
                dto.nombreCoordinador(), dto.idFacultad(), dto.nombreFacultad(), dto.estaActivo());
    }
}
