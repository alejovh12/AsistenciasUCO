package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain.ActualizarProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;

public final class ActualizarProgramaMapper {

    private ActualizarProgramaMapper() {
    }

    public static ActualizarProgramaDomain toDomain(final ActualizarProgramaDTO dto) {
        return new ActualizarProgramaDomain(dto.actorUsuarioId(), dto.id(), dto.nombre(), dto.idTipoPrograma(), dto.idCoordinador(), dto.idModalidad(), dto.idJornada());
    }

    public static ProgramaAcademicoDTO toDto(final ProgramaAcademicoDomain domain) {
        return ProgramaAcademicoMapper.toDto(domain);
    }
}
