package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.RetirarSolicitudIngresoUseCase;
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
 * HU055: el estudiante propietario retira su solicitud abierta (queda RETIRADA y puede volver a solicitar). Una
 * solicitud ajena es indistinguible de una inexistente. Se avisa al docente titular (mejor esfuerzo).
 */
public final class RetirarSolicitudIngresoUseCaseImpl implements RetirarSolicitudIngresoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetirarSolicitudIngresoUseCaseImpl.class);

    private final InstitutionalScopePort scopePort;
    private final SolicitudIngresoGrupoPort port;
    private final NotificacionPort notificacionPort;

    public RetirarSolicitudIngresoUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudIngresoGrupoPort port,
            final NotificacionPort notificacionPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "SolicitudIngresoGrupoPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public void execute(final UUID actorUsuarioId, final UUID solicitudId) {
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        port.retirarSolicitud(solicitudId, actorUsuarioId);
        try {
            final List<DestinatarioSolicitudIngresoProjection> docentes = port.consultarDestinatarios(solicitudId, actorUsuarioId)
                    .stream()
                    .filter(destinatario -> "DOCENTE".equals(destinatario.rol()))
                    .toList();
            if (docentes.isEmpty()) {
                return;
            }
            final DestinatarioSolicitudIngresoProjection contexto = docentes.getFirst();
            final String grupo = contexto.nombreGrupo() + " de " + contexto.nombreAsignatura();
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_SOLICITUD_INGRESO_RETIRADA,
                    "Solicitud de ingreso retirada del grupo " + grupo,
                    "Un estudiante retiro su solicitud de ingreso al grupo " + grupo + ".",
                    docentes.stream()
                            .map(destinatario -> new Notificacion.Destinatario(destinatario.rol(), destinatario.idUsuario(),
                                    destinatario.nombreCompleto(), destinatario.correo()))
                            .toList(),
                    Map.of("idSolicitud", solicitudId.toString())));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible enviar el aviso de retiro de la solicitud {}: {}", solicitudId,
                    exception.getClass().getSimpleName());
        }
    }
}
