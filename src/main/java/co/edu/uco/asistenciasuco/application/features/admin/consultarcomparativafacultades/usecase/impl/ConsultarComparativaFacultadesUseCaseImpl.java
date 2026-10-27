package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.ConsultarComparativaFacultadesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ConsultarComparativaFacultadesDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ComparativaFacultadDomain;
import java.util.List;
import java.util.Objects;

/** El ambito (institucion del administrador) lo fuerza la DB: aqui solo se valida la forma de la consulta. */
public final class ConsultarComparativaFacultadesUseCaseImpl implements ConsultarComparativaFacultadesUseCase {

    private final AdminIndicadoresQueryPort queryPort;

    public ConsultarComparativaFacultadesUseCaseImpl(final AdminIndicadoresQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "AdminIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public List<ComparativaFacultadDomain> execute(final ConsultarComparativaFacultadesDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        return queryPort.consultarComparativa(domain.actorUsuarioId(), domain.idPeriodoAcademico()).stream()
                .map(p -> new ComparativaFacultadDomain(p.idFacultad(), p.nombreFacultad(), p.estaActivaFacultad(), p.totalProgramas(), p.totalEstudiantes(), p.estudiantesRiesgo(), p.estudiantesPerdida(), p.tasaRetencion(), p.sesionesEjecutadas(), p.faltasSinJustaCausa(), p.porcentajeAusentismo()))
                .toList();
    }
}
