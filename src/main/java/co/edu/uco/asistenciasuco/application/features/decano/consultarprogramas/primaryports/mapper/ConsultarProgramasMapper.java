package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ConsultarProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramasPaginaDomain;

public final class ConsultarProgramasMapper {

    private ConsultarProgramasMapper() {
    }

    public static ConsultarProgramasDomain toDomain(final ConsultarProgramasDTO dto) {
        return new ConsultarProgramasDomain(dto.actorUsuarioId(), dto.nombre(), dto.estaActivo(), dto.idTipoPrograma(), dto.pagina(), dto.tamano());
    }

    public static ProgramasPaginaDTO toPagina(final ProgramasPaginaDomain domain) {
        return new ProgramasPaginaDTO(domain.programas().stream().map(ProgramaAcademicoMapper::toDto).toList(), domain.totalRegistros());
    }
}
