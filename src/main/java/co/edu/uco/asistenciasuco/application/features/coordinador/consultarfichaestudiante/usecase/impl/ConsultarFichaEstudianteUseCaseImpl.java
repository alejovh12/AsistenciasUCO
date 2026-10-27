package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.indicadores.rule.IndicadoresRules;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ResumenAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.ConsultarFichaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ConsultarFichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.FichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ResumenAsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List;
import java.util.Objects;

public final class ConsultarFichaEstudianteUseCaseImpl implements ConsultarFichaEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final CoordinadorIndicadoresQueryPort queryPort;

    public ConsultarFichaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "CoordinadorIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public FichaEstudianteDomain execute(final ConsultarFichaEstudianteDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        IndicadoresRules.requerido(domain.idEstudiante(), "El identificador del estudiante es obligatorio.");
        IndicadoresRules.programaDelCoordinador(scopePort, domain.actorUsuarioId());
        final FichaEstudianteProjection f = queryPort.consultarFicha(domain.actorUsuarioId(), domain.idEstudiante());
        final List<ResumenAsignaturaDomain> resumen = queryPort.consultarResumenAsistencia(domain.actorUsuarioId(), domain.idEstudiante(), domain.idPeriodoAcademico()).stream()
                .map(r -> new ResumenAsignaturaDomain(r.idGrupo(), r.codigoGrupo(), r.idAsignatura(), r.nombreAsignatura(), r.codigoEstadoEstudiante(), r.nombreEstadoEstudiante(), r.sesionesEjecutadas(), r.asistencias(), r.faltasSinJustaCausa(), r.excusas(), r.porcentajeInasistencia(), r.codigoPermanencia())).toList();
        return new FichaEstudianteDomain(f.idEstudiante(), f.tipoIdentificacion(), f.numeroIdentificacion(), f.primerNombre(), f.segundoNombre(), f.primerApellido(), f.segundoApellido(), f.nombreCompleto(), f.correo(), f.estaActivoUsuario(), f.idPrograma(), f.nombrePrograma(), resumen);
    }
}
