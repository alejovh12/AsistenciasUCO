package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain.DecanoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;

public final class ConsultarDecanosRepositoryMapper {

    private ConsultarDecanosRepositoryMapper() {
    }

    public static DecanoDomain toDomain(final DecanoProjection projection) {
        return new DecanoDomain(projection.id(), projection.idUsuario(), projection.numeroIdentificacion(),
                projection.nombreCompleto(), projection.idFacultad(), projection.nombreFacultad(),
                projection.estaActivoDecano());
    }
}
