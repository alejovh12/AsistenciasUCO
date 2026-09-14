package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;

public final class ConsultarCoordinadoresRepositoryMapper {

    private ConsultarCoordinadoresRepositoryMapper() {
    }

    public static CoordinadorDomain toDomain(final CoordinadorProjection projection) {
        return new CoordinadorDomain(projection.id(), projection.idUsuario(), projection.numeroIdentificacion(),
                projection.nombreCompleto(), projection.idPrograma(), projection.nombrePrograma(),
                projection.estaActivoCoordinador());
    }
}
