package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.DecanoIndicadoresRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.ConsultarComparativaProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ConsultarComparativaProgramasDomain;
import java.util.List;
import java.util.Objects;

public final class ConsultarComparativaProgramasUseCaseImpl implements ConsultarComparativaProgramasUseCase {

    private final InstitutionalScopePort scopePort;
    private final DecanoIndicadoresQueryPort queryPort;

    public ConsultarComparativaProgramasUseCaseImpl(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DecanoIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public List<ComparativaProgramaDomain> execute(final ConsultarComparativaProgramasDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        DecanoIndicadoresRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        return queryPort.consultarComparativa(domain.actorUsuarioId(), domain.idPeriodoAcademico()).stream()
                .map(p -> new ComparativaProgramaDomain(p.idPrograma(), p.nombrePrograma(), p.estaActivoPrograma(), p.totalEstudiantes(), p.estudiantesRiesgo(), p.estudiantesPerdida(), p.tasaRetencion(), p.sesionesEjecutadas(), p.faltasSinJustaCausa(), p.porcentajeAusentismo()))
                .toList();
    }
}
