package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.EstudiantePadronDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;

public final class ConsultarPadronGrupoRepositoryMapper {

    private ConsultarPadronGrupoRepositoryMapper() {
    }

    public static EstudiantePadronDomain toDomain(final EstudianteGrupoRepositoryProjection p) {
        return new EstudiantePadronDomain(
                p.id(), p.idEstudiante(), p.documento(), p.nombreCompleto(), p.correo(), p.codigoEstado(), p.nombreEstado());
    }
}
