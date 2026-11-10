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
 * evento {@code SESION_CANCELADA} del grupo con {@code tipo = COORDINADOR}. Los estudiantes nunca supieron de la sesion: no se avisan.
 */
public final class RechazarSolicitudSesionUseCaseImpl implements RechazarSolicitudSesionUseCase {

    public static final String EVENTO_SESION_CANCELADA = "SESION_CANCELADA";
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

        final boolean docenteAvisado = avisarDocente(comando);
        final boolean publicado = publicarEvento(rechazada);
        return new Resultado(rechazada.idSesion(), rechazada.idGrupo(), rechazada.mensajeUsuario(), rechazada.totalEstudiantesActivos(),
                docenteAvisado, publicado);
    }

    private boolean avisarDocente(final Comando comando) {
        try {
            final List<DestinatarioSesionManual> destinatarios = repositorio.consultarDestinatariosSolicitud(
                    comando.sesionId(), comando.usuarioEjecutor());
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
        } catch (final RuntimeException exception) {
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

    /** El inicio en UTC solo si la procedencia temporal de la sesion esta confirmada. */
    private Optional<String> confirmada(final SolicitudRechazada rechazada) {
        try {
            return sesiones.consultarSesion(rechazada.idSesion())
                    .filter(s -> EstadoHorarioSesion.desdeProcedencia(s.procedenciaTemporal()) == EstadoHorarioSesion.CONFIRMADA)
                    .map(SesionProcedenciaRepositoryProjection::fechaHoraInicio)
                    .filter(Objects::nonNull)
                    .map(inicio -> inicio.toInstant(ZoneOffset.UTC).toString());
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }
}
