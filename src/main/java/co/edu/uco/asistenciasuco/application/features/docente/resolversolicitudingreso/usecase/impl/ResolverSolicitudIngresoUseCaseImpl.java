package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.ResolverSolicitudIngresoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.domain.ResolverSolicitudIngresoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU053/HU054/HU055: el docente titular acepta o rechaza una solicitud de ingreso. El SP bloquea la fila del grupo y
 * recalcula el cupo: con cupo matricula (ACEPTADA), sin cupo la deja EN_ESPERA; el docente puede aceptar a cualquier
 * solicitud en espera cuando se libera cupo. Se avisa al estudiante (mejor esfuerzo, nunca revierte la resolucion).
 */
public final class ResolverSolicitudIngresoUseCaseImpl implements ResolverSolicitudIngresoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverSolicitudIngresoUseCaseImpl.class);
    private static final String ESTADO_ACEPTADA = "ACEPTADA";
    private static final String ESTADO_EN_ESPERA = "EN_ESPERA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final Map<String, String> ESTADO_LEGIBLE = Map.of("A", ESTADO_ACEPTADA, "E", ESTADO_EN_ESPERA, "R", ESTADO_RECHAZADA);

    private final InstitutionalScopePort scopePort;
    private final SolicitudIngresoGrupoPort port;
    private final NotificacionPort notificacionPort;

    public ResolverSolicitudIngresoUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudIngresoGrupoPort port,
            final NotificacionPort notificacionPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "SolicitudIngresoGrupoPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public String execute(final ResolverSolicitudIngresoDomain domain) {
        scopePort.findDocenteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));

        final String codigo = port.resolverSolicitud(
                domain.solicitudId(), domain.accion(), domain.motivo(), domain.actorUsuarioId());
        final String estado = ESTADO_LEGIBLE.getOrDefault(codigo, codigo);
        avisarAlEstudiante(domain, estado);
        return estado;
    }

    private void avisarAlEstudiante(final ResolverSolicitudIngresoDomain domain, final String estado) {
        try {
            final List<DestinatarioSolicitudIngresoProjection> estudiantes =
                    port.consultarDestinatarios(domain.solicitudId(), domain.actorUsuarioId()).stream()
                            .filter(destinatario -> "ESTUDIANTE".equals(destinatario.rol()))
                            .toList();
            if (estudiantes.isEmpty()) {
                return;
            }
            final DestinatarioSolicitudIngresoProjection contexto = estudiantes.getFirst();
            final String grupo = contexto.nombreGrupo() + " de " + contexto.nombreAsignatura();
            final String mensaje = switch (estado) {
                case ESTADO_ACEPTADA -> "Tu solicitud de ingreso al grupo " + grupo + " fue aceptada: ya estas matriculado.";
                case ESTADO_EN_ESPERA -> "El grupo " + grupo + " no tiene cupo: tu solicitud quedo en lista de espera.";
                default -> "Tu solicitud de ingreso al grupo " + grupo + " fue rechazada. Motivo: " + domain.motivo();
            };
            final String resultado = switch (estado) {
                case ESTADO_ACEPTADA -> "aceptada";
                case ESTADO_EN_ESPERA -> "puesta en espera";
                default -> "rechazada";
            };
            final Map<String, String> datos = new HashMap<>();
            datos.put("idSolicitud", domain.solicitudId().toString());
            datos.put("estado", estado);
            datos.put("grupo", grupo);
            datos.put("resultado", resultado);
            if (domain.motivo() != null) {
                datos.put("motivo", domain.motivo());
            }
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_SOLICITUD_INGRESO_RESUELTA,
                    "Solicitud de ingreso al grupo " + grupo,
                    mensaje,
                    estudiantes.stream()
                            .map(destinatario -> new Notificacion.Destinatario(destinatario.rol(), destinatario.idUsuario(),
                                    destinatario.nombreCompleto(), destinatario.correo()))
                            .toList(),
                    datos));
        } catch (final RuntimeException exception) {
            // La resolucion ya quedo confirmada en la DB: un fallo del aviso se registra pero no se propaga.
            LOGGER.warn("No fue posible enviar el aviso de la solicitud de ingreso {}: {}", domain.solicitudId(),
                    exception.getClass().getSimpleName());
        }
    }
}
