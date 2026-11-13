package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.ConsultarFichaDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.ConsultarFichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.FichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.GrupoHistorialDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.ResumenCargaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.indicadores.rule.IndicadoresRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HistorialGruposDocentePagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU094: ficha del docente para el coordinador. La cabecera sale del directorio del programa (mismo SP, filtrado por
 * docente) y el historial de grupos del SP de historial. Un docente sin vinculo con el programa del coordinador
 * responde 404 igual que uno inexistente (no se revela su existencia).
 */
public final class ConsultarFichaDocenteUseCaseImpl implements ConsultarFichaDocenteUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaQueryPort docenteProgramaQueryPort;
    private final HistorialGruposDocenteQueryPort historialQueryPort;

    public ConsultarFichaDocenteUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaQueryPort docenteProgramaQueryPort,
            final HistorialGruposDocenteQueryPort historialQueryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.docenteProgramaQueryPort = Objects.requireNonNull(docenteProgramaQueryPort, "DocenteProgramaQueryPort es obligatorio.");
        this.historialQueryPort = Objects.requireNonNull(historialQueryPort, "HistorialGruposDocenteQueryPort es obligatorio.");
    }

    @Override
    public FichaDocenteDomain execute(final ConsultarFichaDocenteDomain domain) {
        if (domain == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_011);
        }
        final UUID idDocente = IndicadoresRules.requerido(domain.idDocente(), MensajeUsuarioErrorCode.VAL_APP_074);
        final int pagina = IndicadoresRules.pagina(domain.pagina());
        final int tamano = IndicadoresRules.tamano(domain.tamano());
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_006));

        final DocenteProgramaDomain docente = docenteProgramaQueryPort.consultar(domain.actorUsuarioId(),
                        new DocentesProgramaFiltro(idPrograma, 1, 1, null, null, null, idDocente, null))
                .elementos().stream()
                .findFirst()
                .map(DocenteProgramaMapper::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException(MensajeUsuarioErrorCode.NOF_APP_004));

        final HistorialGruposDocentePagina historial = historialQueryPort.consultarHistorial(
                domain.actorUsuarioId(), idDocente, domain.idPeriodoAcademico(), pagina, tamano);
        final List<GrupoHistorialDocenteDomain> grupos = historial.grupos().stream()
                .map(g -> new GrupoHistorialDocenteDomain(g.idGrupo(), g.codigoGrupo(), g.nombreGrupo(), g.idAsignatura(),
                        g.nombreAsignatura(), g.idPeriodoAcademico(), g.codigoPeriodoAcademico(), g.nombrePeriodoAcademico(),
                        g.fechaInicioPeriodo(), g.fechaFinPeriodo(), g.estadoPeriodo(), g.capacidadMaxima(),
                        g.estudiantesActivos(), g.estudiantesFinalizados(), g.estudiantesCanceladosVoluntad(),
                        g.estudiantesCanceladosInasistencia()))
                .toList();
        return new FichaDocenteDomain(docente,
                new ResumenCargaDocenteDomain(historial.totalGrupos(), historial.totalPeriodos(), historial.gruposVigentes(),
                        historial.estudiantesActivosVigentes()),
                grupos, historial.total());
    }
}
