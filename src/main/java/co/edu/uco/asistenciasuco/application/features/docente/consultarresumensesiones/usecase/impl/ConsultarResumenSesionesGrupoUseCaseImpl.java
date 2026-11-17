package co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.result.ResumenSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.usecase.ConsultarResumenSesionesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/** CIERRE-W3M: la titularidad se valida aqui (403) y de nuevo en {@code usp_consultar_resumen_sesiones_grupo_docente}. */
public final class ConsultarResumenSesionesGrupoUseCaseImpl implements ConsultarResumenSesionesGrupoUseCase {

    private final InstitutionalScopePort scope;
    private final ResumenSesionesGrupoQueryPort resumen;

    public ConsultarResumenSesionesGrupoUseCaseImpl(final InstitutionalScopePort scope, final ResumenSesionesGrupoQueryPort resumen) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.resumen = Objects.requireNonNull(resumen, "ResumenSesionesGrupoQueryPort es obligatorio.");
    }

    @Override
    public ResumenSesionesGrupoDTO execute(final Consulta consulta) {
        if (consulta == null || consulta.grupoId() == null || consulta.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        if (!scope.canDocenteAccessGrupo(consulta.usuarioEjecutor(), consulta.grupoId())) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_002);
        }
        final ResumenSesionesGrupoQueryPort.ResumenGrupo g = resumen.consultarResumenSesionesGrupoDocente(
                consulta.grupoId(), consulta.usuarioEjecutor());
        return new ResumenSesionesGrupoDTO(g.sesionesProgramadas(), g.sesionesEjecutadas(), g.sesionesCanceladas(), g.sesionesEnCurso(),
                g.idSesionEnCurso(), g.idSesionProxima(), g.estudiantesActivos(), g.estudiantesEnRiesgo(), g.promedioAsistencia(),
                g.umbralAdvertencia(), g.umbralPerdida(),
                g.sesiones().stream().map(s -> new ResumenSesionesGrupoDTO.Sesion(s.idSesion(), s.numero(), s.nombre(),
                        s.fechaHoraInicioUtc(), s.fechaHoraFinUtc(), s.horarioConfirmado(), s.estadoSesion(), s.estadoSolicitud(),
                        s.pendienteAprobacion(), s.totalEstudiantes(), s.presentes(), s.parciales(), s.justificadas(),
                        s.ausentes(), s.sinRegistro())).toList());
    }
}
