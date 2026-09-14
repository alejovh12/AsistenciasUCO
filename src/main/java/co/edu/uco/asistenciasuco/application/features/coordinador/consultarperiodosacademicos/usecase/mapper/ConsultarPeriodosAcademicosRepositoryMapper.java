package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;

public final class ConsultarPeriodosAcademicosRepositoryMapper {

    private ConsultarPeriodosAcademicosRepositoryMapper() {
    }

    public static PeriodoAcademicoDomain toDomain(final PeriodoAcademicoProjection projection) {
        return new PeriodoAcademicoDomain(
                projection.id(),
                projection.idInstitucion(),
                projection.nombreInstitucion(),
                projection.nombre(),
                projection.codigo(),
                projection.fechaInicio(),
                projection.fechaFin(),
                projection.anio()
        );
    }
}
