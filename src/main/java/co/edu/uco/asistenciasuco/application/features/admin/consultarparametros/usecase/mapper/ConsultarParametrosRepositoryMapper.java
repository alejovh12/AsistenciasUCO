package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.domain.ParametroDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;

public final class ConsultarParametrosRepositoryMapper {

    private ConsultarParametrosRepositoryMapper() {
    }

    public static ParametroDomain toDomain(final ParametroProjection projection) {
        return new ParametroDomain(projection.id(), projection.grupo(), projection.clave(), projection.valor(),
                projection.tipoDato(), projection.valorDefecto(), projection.estaActivo());
    }
}
