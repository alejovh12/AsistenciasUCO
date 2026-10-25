package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.ConsultarKpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.KpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import java.util.List;

public final class ConsultarKpisCoordinadorMapper {

    private ConsultarKpisCoordinadorMapper() {
    }

    public static ConsultarKpisCoordinadorDomain toDomain(final ConsultarKpisCoordinadorDTO dto) {
        return new ConsultarKpisCoordinadorDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico());
    }

    public static KpisCoordinadorDTO toDto(final KpisCoordinadorDomain d) {
        return new KpisCoordinadorDTO(d.idPrograma(), d.nombrePrograma(), d.idPeriodoAcademico(), d.nombrePeriodoAcademico(), d.totalEstudiantes(), d.estudiantesRegulares(), d.estudiantesRiesgo(), d.estudiantesPerdida(), d.tasaPermanencia(), d.sesionesProgramadas(), d.sesionesVencidas(), d.sesionesEjecutadas(), d.sesionesVencidasSinRegistro(), d.sesionesSinProcedenciaTemporal(), d.porcentajeCumplimiento(), d.porcentajeProgresoCalendario(), d.umbralAdvertencia(), d.umbralPerdida());
    }
}
