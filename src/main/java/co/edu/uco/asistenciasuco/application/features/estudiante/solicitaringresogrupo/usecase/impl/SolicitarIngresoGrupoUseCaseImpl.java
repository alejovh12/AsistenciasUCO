package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.SolicitarIngresoGrupoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU053: el estudiante solicita el ingreso a un grupo; queda PENDIENTE hasta que el docente titular la resuelva. El SP
 * valida grupo, periodo vigente, matricula activa y solicitud abierta duplicada. Se avisa al docente titular.
 */
public final class SolicitarIngresoGrupoUseCaseImpl implements SolicitarIngresoGrupoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolicitarIngresoGrupoUseCaseImpl.class);

    private final InstitutionalScopePort scopePort;
    private final SolicitudIngresoGrupoPort port;
    private final NotificacionPort notificacionPort;

    public SolicitarIngresoGrupoUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudIngresoGrupoPort port,
            final NotificacionPort notificacionPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "SolicitudIngresoGrupoPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public UUID execute(final UUID actorUsuarioId, final UUID grupoId) {
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        final UUID idSolicitud = UUID.randomUUID();
        port.registrarSolicitud(idSolicitud, grupoId, actorUsuarioId);
        avisarAlDocente(idSolicitud, actorUsuarioId);
        return idSolicitud;
    }

    private void avisarAlDocente(final UUID idSolicitud, final UUID actorUsuarioId) {
        try {
            final List<DestinatarioSolicitudIngresoProjection> destinatarios = port.consultarDestinatarios(idSolicitud, actorUsuarioId);
            final List<DestinatarioSolicitudIngresoProjection> docentes = destinatarios.stream()
                    .filter(destinatario -> "DOCENTE".equals(destinatario.rol()))
                    .toList();
            if (docentes.isEmpty()) {
                return;
            }
            final String estudiante = destinatarios.stream()
                    .filter(destinatario -> "ESTUDIANTE".equals(destinatario.rol()))
                    .map(DestinatarioSolicitudIngresoProjection::nombreCompleto)
                    .findFirst()
                    .orElse("Un estudiante");
            final DestinatarioSolicitudIngresoProjection contexto = docentes.getFirst();
            final String grupo = contexto.nombreGrupo() + " de " + contexto.nombreAsignatura();
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_SOLICITUD_INGRESO_REGISTRADA,
                    "Nueva solicitud de ingreso al grupo " + grupo,
                    estudiante + " solicito ingresar al grupo " + grupo + ".",
                    docentes.stream()
                            .map(destinatario -> new Notificacion.Destinatario(destinatario.rol(), destinatario.idUsuario(),
                                    destinatario.nombreCompleto(), destinatario.correo()))
                            .toList(),
                    Map.of("idSolicitud", idSolicitud.toString(), "idGrupo", String.valueOf(contexto.idGrupo()))));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible enviar el aviso de la solicitud de ingreso {}: {}", idSolicitud,
                    exception.getClass().getSimpleName());
        }
    }
}
