package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto.FacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.domain.FacultadDomain;

public final class ConsultarFacultadesMapper {

    private ConsultarFacultadesMapper() {
    }

    public static FacultadDTO toDTO(final FacultadDomain domain) {
        return new FacultadDTO(domain.id(), domain.nombreFacultad(), domain.idInstitucion(), domain.nombreInstitucion(),
                domain.idDecano(), domain.nombreCompletoDecano(), domain.estaActivaFacultad(), domain.estaActivaTextoFacultad());
    }
}
