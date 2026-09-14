package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;

public final class ConsultarPeriodosAcademicosMapper {

    private ConsultarPeriodosAcademicosMapper() {
    }

    public static PeriodoAcademicoDTO toDTO(final PeriodoAcademicoDomain domain) {
        return new PeriodoAcademicoDTO(
                domain.id(),
                domain.idInstitucion(),
                domain.nombreInstitucion(),
                domain.nombre(),
                domain.codigo(),
                domain.fechaInicio(),
                domain.fechaFin(),
                domain.anio()
        );
    }
}
