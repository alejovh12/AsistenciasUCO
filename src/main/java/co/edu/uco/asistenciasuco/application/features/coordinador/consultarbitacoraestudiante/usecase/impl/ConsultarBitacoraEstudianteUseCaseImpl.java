package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.indicadores.rule.IndicadoresRules;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.ConsultarBitacoraEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraSesionDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.ConsultarBitacoraEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;

public final class ConsultarBitacoraEstudianteUseCaseImpl implements ConsultarBitacoraEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final CoordinadorIndicadoresQueryPort queryPort;

    public ConsultarBitacoraEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "CoordinadorIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public BitacoraPaginaDomain execute(final ConsultarBitacoraEstudianteDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        IndicadoresRules.requerido(domain.idEstudiante(), "El identificador del estudiante es obligatorio.");
        final int pagina = IndicadoresRules.pagina(domain.pagina());
        final int tamano = IndicadoresRules.tamano(domain.tamano());
        IndicadoresRules.programaDelCoordinador(scopePort, domain.actorUsuarioId());
        final PaginaBitacoraProjection resultado = queryPort.consultarBitacora(domain.actorUsuarioId(), domain.idEstudiante(), domain.idPeriodoAcademico(), pagina, tamano);
        return new BitacoraPaginaDomain(
                resultado.sesiones().stream().map(s -> new BitacoraSesionDomain(s.idSesion(), s.numeroSesion(), s.nombreSesion(), s.fechaHoraInicio(), s.fechaHoraFin(), s.idGrupo(), s.codigoGrupo(), s.idAsignatura(), s.nombreAsignatura(), s.asistio(), s.codigoRazonCausa(), s.nombreRazonCausa())).toList(),
                resultado.totalRegistros());
    }
}
