package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain.CambiarEstadoProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;

public final class CambiarEstadoProgramaMapper {

    private CambiarEstadoProgramaMapper() {
    }

    public static CambiarEstadoProgramaDomain toDomain(final CambiarEstadoProgramaDTO dto) {
        return new CambiarEstadoProgramaDomain(dto.actorUsuarioId(), dto.id(), dto.estaActivo());
    }

    public static ProgramaAcademicoDTO toDto(final ProgramaAcademicoDomain domain) {
        return ProgramaAcademicoMapper.toDto(domain);
    }
}
