package co.edu.uco.asistenciasuco.application.features.decano.common.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoProjection;

public final class ProgramaAcademicoMapper {

    private ProgramaAcademicoMapper() {
    }

    public static ProgramaAcademicoDomain toDomain(final ProgramaAcademicoProjection p) {
        return new ProgramaAcademicoDomain(p.id(), p.nombre(), p.idTipoPrograma(), p.nombreTipoPrograma(),
                p.idModalidad(), p.nombreModalidad(), p.idJornada(), p.nombreJornada(), p.idCoordinador(),
                p.nombreCoordinador(), p.idFacultad(), p.nombreFacultad(), p.estaActivo());
    }

    public static ProgramaAcademicoDTO toDto(final ProgramaAcademicoDomain d) {
        return new ProgramaAcademicoDTO(d.id(), d.nombre(), d.idTipoPrograma(), d.nombreTipoPrograma(),
                d.idModalidad(), d.nombreModalidad(), d.idJornada(), d.nombreJornada(), d.idCoordinador(),
                d.nombreCoordinador(), d.idFacultad(), d.nombreFacultad(), d.estaActivo());
    }
}
