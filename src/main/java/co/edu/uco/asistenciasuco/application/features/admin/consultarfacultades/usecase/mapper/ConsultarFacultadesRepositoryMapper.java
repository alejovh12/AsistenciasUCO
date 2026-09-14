package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.domain.FacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;

public final class ConsultarFacultadesRepositoryMapper {

    private ConsultarFacultadesRepositoryMapper() {
    }

    public static FacultadDomain toDomain(final FacultadProjection projection) {
        return new FacultadDomain(projection.id(), projection.nombreFacultad(), projection.idInstitucion(),
                projection.nombreInstitucion(), projection.idDecano(), projection.nombreCompletoDecano(),
                projection.estaActivaFacultad(), projection.estaActivaTextoFacultad());
    }
}
