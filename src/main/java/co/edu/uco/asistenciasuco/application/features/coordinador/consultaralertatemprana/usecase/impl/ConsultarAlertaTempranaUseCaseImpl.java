package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.rule.IndicadoresRules;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.ConsultarAlertaTempranaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.ConsultarAlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;

public final class ConsultarAlertaTempranaUseCaseImpl implements ConsultarAlertaTempranaUseCase {

    private final InstitutionalScopePort scopePort;
    private final CoordinadorIndicadoresQueryPort queryPort;

    public ConsultarAlertaTempranaUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "CoordinadorIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public AlertasPaginaDomain execute(final ConsultarAlertaTempranaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los filtros de consulta son obligatorios.");
        }
        final int pagina = IndicadoresRules.pagina(domain.pagina());
        final int tamano = IndicadoresRules.tamano(domain.tamano());
        final String semaforo = IndicadoresRules.textoOpcional(domain.semaforo());
        if (semaforo != null && !IndicadoresRules.SEMAFOROS.contains(semaforo)) {
            throw new ValidationException("El semaforo debe ser VERDE, AMBAR o ROJO.");
        }
        IndicadoresRules.programaDelCoordinador(scopePort, domain.actorUsuarioId());
        final PaginaAlertasProjection resultado = queryPort.consultarAlertas(domain.actorUsuarioId(), domain.idPeriodoAcademico(), semaforo,
                IndicadoresRules.textoOpcional(domain.nombre()), pagina, tamano);
        return new AlertasPaginaDomain(
                resultado.alertas().stream().map(a -> new AlertaTempranaDomain(a.idEstudiante(), a.nombreCompleto(), a.numeroIdentificacion(), a.correo(), a.totalMatriculasActivas(), a.porcentajeInasistencia(), a.faltasSinJustaCausa(), a.codigoPermanencia(), a.semaforo())).toList(),
                resultado.totalRegistros());
    }
}
