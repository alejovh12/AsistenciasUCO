package co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.usecase.RechazarSolicitudSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudRechazada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * DEC-065 (2): el coordinador rechaza la solicitud de sesion extraordinaria; la sesion queda cancelada con el motivo del catalogo.
 * El rol, el ambito, el motivo ({@code SES_019}) y la observacion ({@code SES_021}) los valida la DB. Tras el commit del SP y de mejor
 * esfuerzo: UNA notificacion {@code SESION_SOLICITUD_RECHAZADA_DOCENTE} al docente (con motivo y observacion en los datos) y el
 * evento {@code SESION_CANCELADA} del grupo con {@code tipo = COORDINADOR} y, para el menu de solicitudes (ATA D6), el evento
 * {@code SESION_SOLICITUD_RECHAZADA} (mismo payload que {@code SESION_SOLICITUD_CREADA}). Los estudiantes nunca supieron de la sesion:
 * no se avisan.
 */
public final class RechazarSolicitudSesionUseCaseImpl implements RechazarSolicitudSesionUseCase {

    public static final String EVENTO_SESION_CANCELADA = "SESION_CANCELADA";
    /** ATA D6: la solicitud del docente fue rechazada (audiencia: ambito del grupo + docente titular; nunca los estudiantes). */
    public static final String EVENTO_SESION_SOLICITUD_RECHAZADA = "SESION_SOLICITUD_RECHAZADA";
    static final String TIPO_CANCELACION_COORDINADOR = "COORDINADOR";
    private static final Logger LOGGER = LoggerFactory.getLogger(RechazarSolicitudSesionUseCaseImpl.class);

    private final SesionProcedenciaQueryPort sesiones;
    private final SesionManualRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public RechazarSolicitudSesionUseCaseImpl(
            final SesionProcedenciaQueryPort sesiones,
            final SesionManualRepositoryPort repositorio,
            final NotificacionPort notificaciones,
            final RealtimePublisherPort realtime
    ) {
        this.sesiones = Objects.requireNonNull(sesiones, "SesionProcedenciaQueryPort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "SesionManualRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para rechazar la solicitud es obligatorio.");
        final SolicitudRechazada rechazada = repositorio.rechazarSolicitud(
                comando.sesionId(), comando.codigoMotivo(), comando.observacion(), comando.usuarioEjecutor());

        final List<DestinatarioSesionManual> destinatarios = consultarDestinatarios(comando);
        final boolean docenteAvisado = avisarDocente(comando, destinatarios);
        final boolean publicado = publicarEvento(rechazada);
        publicarSolicitudRechazada(rechazada, destinatarios);
        return new Resultado(rechazada.idSesion(), rechazada.idGrupo(), rechazada.mensajeUsuario(), rechazada.totalEstudiantesActivos(),
                docenteAvisado, publicado);
    }

    /** Destinatarios de la solicitud; de mejor esfuerzo: si la consulta falla, lista vacia. */
    private List<DestinatarioSesionManual> consultarDestinatarios(final Comando comando) {
        try {
            return repositorio.consultarDestinatariosSolicitud(comando.sesionId(), comando.usuarioEjecutor());
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los destinatarios de la solicitud {}: {}", comando.sesionId(),
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private boolean avisarDocente(final Comando comando, final List<DestinatarioSesionManual> destinatarios) {
        try {
            final List<DestinatarioSesionManual> docentes = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_DOCENTE);
            if (docentes.isEmpty()) {
                return false;
            }
            notificaciones.notificar(avisoDocente(comando, docentes.getFirst()));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar el rechazo de la solicitud {}: {}", comando.sesionId(), exception.getClass().getSimpleName());
            return false;
        }
    }

    private Notificacion avisoDocente(final Comando comando, final DestinatarioSesionManual docente) {
        final Map<String, String> datos = AvisoSesionSupport.datosBase(comando.sesionId(), docente);
        final String motivo = nombreMotivo(comando);
        final String observacion = comando.observacion() == null ? "" : comando.observacion().strip();
        datos.put("docente", AvisoSesionSupport.valor(docente.nombreCompleto()));
        datos.put("coordinador", AvisoSesionSupport.COORDINADOR_DEL_PROGRAMA);
        datos.put("motivo", motivo);
        datos.put("observacion", observacion);
        return new Notificacion(
                Notificacion.TIPO_SESION_SOLICITUD_RECHAZADA_DOCENTE,
                "Solicitud rechazada: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")",
                ("Hola " + datos.get("docente") + ". " + AvisoSesionSupport.COORDINADOR_DEL_PROGRAMA
                        + " rechazó su solicitud de sesión extraordinaria de " + datos.get("asignatura")
                        + AvisoSesionSupport.ventana(datos) + " Motivo: " + motivo + ". " + observacion).stripTrailing(),
                AvisoSesionSupport.comoDestinatarios(List.of(docente)), datos);
    }

    /** Nombre del motivo en el catalogo; si no se puede leer, el codigo recibido. */
    private String nombreMotivo(final Comando comando) {
        try {
            return repositorio.consultarMotivosCancelacion(comando.usuarioEjecutor()).stream()
                    .filter(m -> Objects.equals(m.codigo(), comando.codigoMotivo()))
                    .map(MotivoCancelacion::nombre)
                    .findFirst()
                    .orElse(AvisoSesionSupport.valor(comando.codigoMotivo()));
        } catch (final RuntimeException _) {
            return AvisoSesionSupport.valor(comando.codigoMotivo());
        }
    }

    private boolean publicarEvento(final SolicitudRechazada rechazada) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", rechazada.idGrupo().toString());
        payload.put("sesion", rechazada.idSesion().toString());
        payload.put("tipo", TIPO_CANCELACION_COORDINADOR);
        confirmada(rechazada).ifPresent(inicio -> payload.put("fechaHoraInicioUtc", inicio));
        try {
            realtime.publish(RealtimeEvent.of(EVENTO_SESION_CANCELADA, payload));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento de la solicitud rechazada {}: {}", rechazada.idSesion(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    /** ATA D6: aviso en tiempo real al menu de solicitudes del coordinador y del docente; de mejor esfuerzo. */
    private void publicarSolicitudRechazada(final SolicitudRechazada rechazada, final List<DestinatarioSesionManual> destinatarios) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", rechazada.idGrupo().toString());
        payload.put("sesion", rechazada.idSesion().toString());
        payload.put("docente", destinatarios.stream().map(DestinatarioSesionManual::nombreDocente).filter(Objects::nonNull)
                .findFirst().orElse(""));
        try {
            sesiones.consultarSesion(rechazada.idSesion()).ifPresent(s -> payload.put("fechaHoraInicio", AvisoSesionSupport.fechaHoraInicio(s)));
        } catch (final RuntimeException _) {
            // sin hora: el evento solo recarga la bandeja
        }
        try {
            realtime.publish(RealtimeEvent.of(EVENTO_SESION_SOLICITUD_RECHAZADA, payload));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento {} de la solicitud {}: {}", EVENTO_SESION_SOLICITUD_RECHAZADA,
                    rechazada.idSesion(), exception.getClass().getSimpleName());
        }
    }

    /** El inicio en UTC solo si la procedencia temporal de la sesion esta confirmada. */
    private Optional<String> confirmada(final SolicitudRechazada rechazada) {
        try {
            return sesiones.consultarSesion(rechazada.idSesion())
                    .filter(s -> EstadoHorarioSesion.desdeProcedencia(s.procedenciaTemporal()) == EstadoHorarioSesion.CONFIRMADA)
                    .map(SesionProcedenciaRepositoryProjection::fechaHoraInicio)
                    .filter(Objects::nonNull)
                    .map(inicio -> inicio.toInstant(ZoneOffset.UTC).toString());
        } catch (final RuntimeException _) {
            return Optional.empty();
        }
    }
}
