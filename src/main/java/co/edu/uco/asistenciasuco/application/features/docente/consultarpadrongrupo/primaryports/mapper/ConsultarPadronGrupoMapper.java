package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.ConsultarPadronGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.EstudiantePadronDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.PadronGrupoPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;

public final class ConsultarPadronGrupoMapper {

    private ConsultarPadronGrupoMapper() {
    }

    public static ConsultarPadronGrupoDomain toDomain(final ConsultarPadronGrupoDTO dto) {
        return ConsultarPadronGrupoDomain.of(
                dto == null ? null : dto.grupoId(),
                dto == null ? null : dto.usuarioEjecutor(),
                dto == null ? null : dto.pagina(),
                dto == null ? null : dto.tamano(),
                dto == null ? null : dto.busqueda());
    }

    public static PadronGrupoPaginaDTO toDTO(final PadronGrupoPaginaDomain domain) {
        return new PadronGrupoPaginaDTO(
                domain.estudiantes().stream().map(ConsultarPadronGrupoMapper::toDTO).toList(),
                domain.pagina(), domain.tamano(), domain.totalRegistros());
    }

    private static EstudianteGrupoDTO toDTO(final EstudiantePadronDomain p) {
        return new EstudianteGrupoDTO(
                p.id(), p.idEstudiante(), p.documento(), p.nombreCompleto(), p.correo(), p.codigoEstado(), p.nombreEstado());
    }
}
