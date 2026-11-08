package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadResolutores;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.Resolutor;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.DestinatarioRevision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C, contrato 3.5 y 3.6.4): avisos y eventos de la revision de cancelacion tras una decision del docente, una
 * resolucion o una reasignacion del job. Los destinatarios los calcula la DB ({@code usp_consultar_destinatarios_revision_cancelacion}:
 * el proponente nunca es resolutor). Mejor esfuerzo: un fallo se registra y no cambia la respuesta ni el estado ya confirmado.
 */
public final class AvisoRevisionCancelacionService {

    public static final String EVENTO_DECIDIDA = "REVISION_CANCELACION_DECIDIDA";
    public static final String EVENTO_RESUELTA = "REVISION_CANCELACION_RESUELTA";
    public static final String EVENTO_REASIGNADA = "REVISION_CANCELACION_REASIGNADA";
    static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    static final String ROL_DOCENTE = "DOCENTE";
    static final String ROL_RESOLUTOR = "RESOLUTOR";
    private static final String TEXTO_ESCALAMIENTO = "Se le asigna porque el programa no tiene coordinador disponible.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvisoRevisionCancelacionService.class);

    private final RevisionCancelacionRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public AvisoRevisionCancelacionService(
            final RevisionCancelacionRepositoryPort repositorio,
            final NotificacionPort notificaciones,
            final RealtimePublisherPort realtime
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    public void avisarDecision(final ResultadoDecision r, final UUID usuarioEjecutor) {
        if (r.sinCambios()) {
            return;
        }
        try {
            final List<DestinatarioRevision> destinatarios = repositorio.consultarDestinatarios(usuarioEjecutor, r.idRevision());
            final boolean propone = r.tipoPlantillaNotificacion() != null;
            final Set<UUID> audiencia = new HashSet<>();
            destinatarios.stream().filter(d -> ROL_DOCENTE.equals(d.rol()) || (propone && ROL_RESOLUTOR.equals(d.rol())))
                    .forEach(d -> audiencia.add(d.idUsuario()));
            final Map<String, Object> payload = new HashMap<>();
            payload.put("grupo", r.idGrupo().toString());
            payload.put("revision", r.idRevision().toString());
            payload.put("estudianteGrupo", r.idEstudianteGrupo().toString());
            payload.put("decision", r.decision());
            payload.put("estado", r.estadoNuevo());
            if (r.perfilResolutor() != null) {
                payload.put("perfilResolutor", r.perfilResolutor());
            }
            publicar(EVENTO_DECIDIDA, payload, audiencia);
            if (propone) {
                for (final DestinatarioRevision d : destinatarios) {
                    if (ROL_RESOLUTOR.equals(d.rol())) {
                        enviar(avisoPropuesta(d, r.tipoPlantillaNotificacion(), r.escalamiento()));
                    }
                }
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la decision de la revision {}: {}", r.idRevision(), exception.getClass().getSimpleName());
        }
    }

    public void avisarResolucion(final ResultadoResolucion r, final UUID usuarioEjecutor) {
        if (r.sinCambios()) {
            return;
        }
        try {
            final List<DestinatarioRevision> destinatarios = repositorio.consultarDestinatarios(usuarioEjecutor, r.idRevision());
            final Set<UUID> audiencia = new HashSet<>();
            for (final DestinatarioRevision d : destinatarios) {
                if (!ROL_ESTUDIANTE.equals(d.rol()) || r.cambioMatricula()) {
                    audiencia.add(d.idUsuario());
                }
            }
            final Map<String, Object> payload = new HashMap<>();
            payload.put("grupo", r.idGrupo().toString());
            payload.put("revision", r.idRevision().toString());
            payload.put("estudianteGrupo", r.idEstudianteGrupo().toString());
            payload.put("decision", r.decision());
            payload.put("estado", r.estadoNuevo());
            payload.put("cambioMatricula", r.cambioMatricula());
            publicar(EVENTO_RESUELTA, payload, audiencia);
            if (r.tipoPlantillaNotificacion() != null) {
                for (final DestinatarioRevision d : destinatarios) {
                    final boolean aprobada = Notificacion.TIPO_CANCELACION_APROBADA.equals(r.tipoPlantillaNotificacion());
                    if (ROL_DOCENTE.equals(d.rol()) || (aprobada && ROL_ESTUDIANTE.equals(d.rol()))) {
                        enviar(avisoResolucion(d, r.tipoPlantillaNotificacion()));
                    }
                }
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la resolucion de la revision {}: {}", r.idRevision(), exception.getClass().getSimpleName());
        }
    }

    /** Job: propuesta a los resolutores nuevos y evento a docente y resolutores (reasignacion o escalamiento). */
    public void avisarReasignacion(final NovedadResolutores n) {
        try {
            final Set<UUID> audiencia = new HashSet<>();
            if (n.idUsuarioDocente() != null) {
                audiencia.add(n.idUsuarioDocente());
            }
            for (final Resolutor resolutor : n.resolutores()) {
                audiencia.add(resolutor.idUsuario());
            }
            final Map<String, Object> payload = new HashMap<>();
            payload.put("grupo", n.idGrupo().toString());
            payload.put("revision", n.idRevision().toString());
            payload.put("estudianteGrupo", n.idEstudianteGrupo().toString());
            if (n.perfilResolutor() != null) {
                payload.put("perfilResolutor", n.perfilResolutor());
            }
            publicar(EVENTO_REASIGNADA, payload, audiencia);
            if (n.perfilResolutor() != null) {
                final boolean escalada = "DECANO".equals(n.perfilResolutor());
                for (final Resolutor resolutor : n.resolutores()) {
                    final Map<String, String> datos = datosPropuesta(resolutor.nombreCompleto(), n.nombreEstudiante(), n.nombreGrupo(), n.nombreAsignatura(),
                            n.nombreDocente(), n.porcentaje() == null ? "" : n.porcentaje().toPlainString(),
                            n.umbral() == null ? "" : n.umbral().toPlainString(), n.motivo(), escalada);
                    enviar(new Notificacion(Notificacion.TIPO_PROPUESTA_CANCELACION, "Propuesta de cancelación por inasistencia: " + datos.get("estudiante")
                            + " en " + datos.get("grupo"), "Hola " + datos.get("resolutor") + ". Tienes una propuesta de cancelación por inasistencia pendiente de "
                            + "resolver para " + datos.get("estudiante") + " en " + datos.get("grupo") + ". " + datos.get("escalamiento"),
                            List.of(new Notificacion.Destinatario(resolutor.codigoPerfil(), resolutor.idUsuario(), resolutor.nombreCompleto(), resolutor.correo())), datos));
                }
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la reasignacion de la revision {}: {}", n.idRevision(), exception.getClass().getSimpleName());
        }
    }

    private void publicar(final String tipo, final Map<String, Object> payload, final Set<UUID> audiencia) {
        try {
            realtime.publish(RealtimeEvent.paraDestinatarios(tipo, payload, audiencia));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento {}: {}", tipo, exception.getClass().getSimpleName());
        }
    }

    private void enviar(final Notificacion notificacion) {
        try {
            notificaciones.notificar(notificacion);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible encolar el aviso {}: {}", notificacion.tipo(), exception.getClass().getSimpleName());
        }
    }

    private static Notificacion avisoPropuesta(final DestinatarioRevision d, final String tipo, final boolean escalada) {
        final String grupo = texto(d.nombreGrupo()) + " de " + texto(d.nombreAsignatura());
        final Map<String, String> datos = datosPropuesta(d.nombreCompleto(), d.nombreEstudiante(), d.nombreGrupo(), d.nombreAsignatura(), d.nombreDocente(),
                d.porcentajeActual() == null ? "" : d.porcentajeActual().toPlainString(),
                d.umbralPerdida() == null ? "" : d.umbralPerdida().toPlainString(), d.motivoDocente(), escalada);
        return new Notificacion(tipo, "Propuesta de cancelación por inasistencia: " + texto(d.nombreEstudiante()) + " en " + grupo,
                "Hola " + texto(d.nombreCompleto()) + ". El docente " + texto(d.nombreDocente()) + " propone cancelar por inasistencia a "
                        + texto(d.nombreEstudiante()) + " en " + grupo + ". Motivo: " + texto(d.motivoDocente()) + ". " + datos.get("escalamiento"),
                List.of(new Notificacion.Destinatario(d.codigoPerfil(), d.idUsuario(), d.nombreCompleto(), d.correo())), datos);
    }

    private static Map<String, String> datosPropuesta(final String resolutor, final String estudiante, final String grupo, final String asignatura,
                                                      final String docente, final String porcentaje, final String limite, final String motivo,
                                                      final boolean escalada) {
        final Map<String, String> datos = new HashMap<>();
        datos.put("resolutor", texto(resolutor));
        datos.put("estudiante", texto(estudiante));
        datos.put("grupo", texto(grupo) + " de " + texto(asignatura));
        datos.put("docente", texto(docente));
        datos.put("porcentaje", porcentaje);
        datos.put("limite", limite);
        datos.put("motivo", texto(motivo));
        datos.put("escalamiento", escalada ? TEXTO_ESCALAMIENTO : "");
        return datos;
    }

    private static Notificacion avisoResolucion(final DestinatarioRevision d, final String tipo) {
        final String grupo = texto(d.nombreGrupo()) + " de " + texto(d.nombreAsignatura());
        final Map<String, String> datos = new HashMap<>();
        datos.put("destinatario", texto(d.nombreCompleto()));
        datos.put("docente", texto(d.nombreCompleto()));
        datos.put("estudiante", texto(d.nombreEstudiante()));
        datos.put("grupo", grupo);
        datos.put("porcentaje", d.porcentajeActual() == null ? "" : d.porcentajeActual().toPlainString());
        datos.put("motivo", texto(d.motivoResolucion()));
        datos.put("resolutor", texto(d.nombreResolutor()));
        final boolean aprobada = Notificacion.TIPO_CANCELACION_APROBADA.equals(tipo);
        final String asunto = aprobada ? "Cancelación por inasistencia en " + grupo
                : "Propuesta de cancelación rechazada: " + texto(d.nombreEstudiante()) + " en " + grupo;
        final String mensaje = "Hola " + texto(d.nombreCompleto()) + ". " + (aprobada
                ? "Se aprobó la cancelación por inasistencia de " + texto(d.nombreEstudiante()) + " en " + grupo + "."
                : "Se rechazó la propuesta de cancelación de " + texto(d.nombreEstudiante()) + " en " + grupo + "; el estudiante sigue activo.")
                + " Motivo: " + texto(d.motivoResolucion()) + ".";
        return new Notificacion(tipo, asunto, mensaje,
                List.of(new Notificacion.Destinatario(d.codigoPerfil(), d.idUsuario(), d.nombreCompleto(), d.correo())), datos);
    }

    private static String texto(final String valor) {
        return valor == null ? "" : valor;
    }
}
