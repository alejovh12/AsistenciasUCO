package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;

public final class ConsultarAsignaturasPlanMapper {

    private ConsultarAsignaturasPlanMapper() {
    }

    public static AsignaturaDTO toDTO(final AsignaturaDomain domain) {
        return new AsignaturaDTO(
                domain.id(),
                domain.codigo(),
                domain.nombre(),
                domain.credito(),
                domain.idArea(),
                domain.nombreArea(),
                domain.idComponente(),
                domain.nombreComponente(),
                domain.idSemestrePlanEstudio(),
                domain.idPlanEstudio(),
                domain.idPrograma(),
                domain.nombrePrograma(),
                domain.codigoSemestre(),
                domain.estaActivaAsignatura(),
                domain.estaActivaTextoAsignatura()
        );
    }
}
