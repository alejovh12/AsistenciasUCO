package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;

public final class CrearProgramaMapper {

    private CrearProgramaMapper() {
    }

    public static CrearProgramaDomain toDomain(final CrearProgramaDTO dto) {
        return new CrearProgramaDomain(dto.actorUsuarioId(), dto.nombre(), dto.idTipoPrograma(), dto.idCoordinador(), dto.idModalidad(), dto.idJornada());
    }

    public static ProgramaAcademicoDTO toDto(final ProgramaAcademicoDomain domain) {
        return ProgramaAcademicoMapper.toDto(domain);
    }
}
