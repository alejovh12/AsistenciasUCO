package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.domain.InstitucionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;

public final class ConsultarInstitucionesRepositoryMapper {

    private ConsultarInstitucionesRepositoryMapper() {
    }

    public static InstitucionDomain toDomain(final InstitucionProjection projection) {
        return new InstitucionDomain(projection.id(), projection.nombre(), projection.estaActivaInstitucion(),
                projection.estaActivaTextoInstitucion());
    }
}
