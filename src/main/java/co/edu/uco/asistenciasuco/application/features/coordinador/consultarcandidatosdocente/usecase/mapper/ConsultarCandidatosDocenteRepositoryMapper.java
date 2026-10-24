package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.domain.DocenteCandidatoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteCandidatoProjection;

public final class ConsultarCandidatosDocenteRepositoryMapper {

    private ConsultarCandidatosDocenteRepositoryMapper() {
    }

    public static DocenteCandidatoDomain toDomain(final DocenteCandidatoProjection projection) {
        return new DocenteCandidatoDomain(projection.idDocente(), projection.idUsuario(),
                projection.codigoTipoIdentificacion(), projection.numeroIdentificacion(), projection.nombreCompleto());
    }
}
