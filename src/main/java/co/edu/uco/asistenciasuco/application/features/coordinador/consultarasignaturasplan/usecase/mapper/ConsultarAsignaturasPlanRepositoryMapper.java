package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;

public final class ConsultarAsignaturasPlanRepositoryMapper {

    private ConsultarAsignaturasPlanRepositoryMapper() {
    }

    public static AsignaturaDomain toDomain(final AsignaturaProjection projection) {
        return new AsignaturaDomain(
                projection.id(),
                projection.codigo(),
                projection.nombre(),
                projection.credito(),
                projection.idArea(),
                projection.nombreArea(),
                projection.idComponente(),
                projection.nombreComponente(),
                projection.idSemestrePlanEstudio(),
                projection.idPlanEstudio(),
                projection.idPrograma(),
                projection.nombrePrograma(),
                projection.codigoSemestre(),
                projection.estaActivaAsignatura(),
                projection.estaActivaTextoAsignatura()
        );
    }
}
