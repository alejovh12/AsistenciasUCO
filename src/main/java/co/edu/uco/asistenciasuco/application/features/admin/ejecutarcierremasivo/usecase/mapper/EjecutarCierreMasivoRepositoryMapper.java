package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;

public final class EjecutarCierreMasivoRepositoryMapper {

    private EjecutarCierreMasivoRepositoryMapper() {
    }

    public static String toCodigoPeriodo(final PeriodoAcademicoProjection periodo) {
        return periodo.codigo();
    }
}
