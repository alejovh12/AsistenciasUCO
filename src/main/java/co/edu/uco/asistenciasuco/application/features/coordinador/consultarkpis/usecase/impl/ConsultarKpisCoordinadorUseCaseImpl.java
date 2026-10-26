package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.indicadores.rule.IndicadoresRules;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.ConsultarKpisCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.ConsultarKpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.KpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;

public final class ConsultarKpisCoordinadorUseCaseImpl implements ConsultarKpisCoordinadorUseCase {

    private final InstitutionalScopePort scopePort;
    private final CoordinadorIndicadoresQueryPort queryPort;

    public ConsultarKpisCoordinadorUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "CoordinadorIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public KpisCoordinadorDomain execute(final ConsultarKpisCoordinadorDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        IndicadoresRules.programaDelCoordinador(scopePort, domain.actorUsuarioId());
        final KpisCoordinadorProjection p = queryPort.consultarKpis(domain.actorUsuarioId(), domain.idPeriodoAcademico());
        return new KpisCoordinadorDomain(p.idPrograma(), p.nombrePrograma(), p.idPeriodoAcademico(), p.nombrePeriodoAcademico(), p.totalEstudiantes(), p.estudiantesRegulares(), p.estudiantesRiesgo(), p.estudiantesPerdida(), p.tasaPermanencia(), p.sesionesProgramadas(), p.sesionesVencidas(), p.sesionesEjecutadas(), p.sesionesVencidasSinRegistro(), p.sesionesSinProcedenciaTemporal(), p.porcentajeCumplimiento(), p.porcentajeProgresoCalendario(), p.umbralAdvertencia(), p.umbralPerdida());
    }
}
