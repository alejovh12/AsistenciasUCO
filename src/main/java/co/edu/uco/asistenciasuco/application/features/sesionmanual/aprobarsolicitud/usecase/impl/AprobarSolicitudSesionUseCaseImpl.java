package co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.usecase.AprobarSolicitudSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudAprobada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * DEC-065 (2): el coordinador aprueba la solicitud de sesion extraordinaria. El rol, el ambito, que exista la solicitud y que siga
 * pendiente los valida la DB. Tras el commit del SP y de mejor esfuerzo (un fallo se registra y no cambia la respuesta):
 * <ul>
 *   <li>UNA notificacion {@code SESION_MANUAL_CREADA_ESTUDIANTE} con todos los estudiantes activos del grupo;</li>
 *   <li>UNA notificacion {@code SESION_SOLICITUD_APROBADA_DOCENTE} al docente titular;</li>
 *   <li>el evento {@code SESION_MANUAL_CREADA} del grupo (audiencia: estudiantes, ambito y docente titular).</li>
 * </ul>
 */
public final class AprobarSolicitudSesionUseCaseImpl implements AprobarSolicitudSesionUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AprobarSolicitudSesionUseCaseImpl.class);

    private final SesionProcedenciaQueryPort sesiones;
    private final SesionManualRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public AprobarSolicitudSesionUseCaseImpl(
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
        Objects.requireNonNull(comando, "El comando para aprobar la solicitud es obligatorio.");
        final SolicitudAprobada aprobada = repositorio.aprobarSolicitud(comando.sesionId(), comando.observacion(), comando.usuarioEjecutor());

        final List<DestinatarioSesionManual> destinatarios = consultarDestinatarios(comando);
        final List<DestinatarioSesionManual> estudiantes = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_ESTUDIANTE);
        final List<DestinatarioSesionManual> docentes = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_DOCENTE);

        final int avisadosEstudiantes = encolar(comando.sesionId(), AvisoSesionSupport.avisoSesionCreada(comando.sesionId(), estudiantes, true))
                ? estudiantes.size() : 0;
        final boolean docenteAvisado = encolar(comando.sesionId(), avisoDocente(comando.sesionId(), docentes));
        final boolean publicado = publicarEvento(aprobada, destinatarios);
        return new Resultado(aprobada.idSesion(), aprobada.idGrupo(), aprobada.mensajeUsuario(), aprobada.totalEstudiantesActivos(),
                avisadosEstudiantes, docenteAvisado, publicado);
    }

    private List<DestinatarioSesionManual> consultarDestinatarios(final Comando comando) {
        try {
            return repositorio.consultarDestinatariosSolicitud(comando.sesionId(), comando.usuarioEjecutor());
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los destinatarios de la solicitud aprobada {}: {}", comando.sesionId(),
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private boolean encolar(final UUID sesionId, final Optional<Notificacion> aviso) {
        if (aviso.isEmpty()) {
            return false;
        }
        try {
            notificaciones.notificar(aviso.get());
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la solicitud aprobada {} ({}): {}", sesionId, aviso.get().tipo(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    private static Optional<Notificacion> avisoDocente(final UUID sesionId, final List<DestinatarioSesionManual> docentes) {
        if (docentes.isEmpty()) {
            return Optional.empty();
        }
        final DestinatarioSesionManual docente = docentes.getFirst();
        final Map<String, String> datos = AvisoSesionSupport.datosBase(sesionId, docente);
        datos.put("docente", AvisoSesionSupport.valor(docente.nombreCompleto()));
        datos.put("coordinador", AvisoSesionSupport.COORDINADOR_DEL_PROGRAMA);
        return Optional.of(new Notificacion(
                Notificacion.TIPO_SESION_SOLICITUD_APROBADA_DOCENTE,
                "Solicitud aprobada: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")",
                "Hola " + datos.get("docente") + ". " + AvisoSesionSupport.COORDINADOR_DEL_PROGRAMA
                        + " aprobó su solicitud de sesión extraordinaria de " + datos.get("asignatura") + AvisoSesionSupport.ventana(datos)
                        + " Ya puede tomar asistencia en ella y los estudiantes fueron notificados.",
                AvisoSesionSupport.comoDestinatarios(List.of(docente)), datos));
    }

    private boolean publicarEvento(final SolicitudAprobada aprobada, final List<DestinatarioSesionManual> destinatarios) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", aprobada.idGrupo().toString());
        payload.put("sesion", aprobada.idSesion().toString());
        payload.put("fechaHoraInicio", fechaHoraInicio(aprobada));
        payload.put("docente", destinatarios.stream().map(DestinatarioSesionManual::nombreDocente).filter(Objects::nonNull)
                .findFirst().orElse(""));
        try {
            realtime.publish(RealtimeEvent.of(NOMBRE_EVENTO, payload));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento de la solicitud aprobada {}: {}", aprobada.idSesion(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    /** Igual que al crear una sesion: ISO con {@code Z} si la procedencia esta confirmada; si no se puede leer, el valor almacenado. */
    private String fechaHoraInicio(final SolicitudAprobada aprobada) {
        try {
            return sesiones.consultarSesion(aprobada.idSesion()).map(AvisoSesionSupport::fechaHoraInicio)
                    .orElseGet(() -> valorAlmacenado(aprobada));
        } catch (final RuntimeException exception) {
            return valorAlmacenado(aprobada);
        }
    }

    private static String valorAlmacenado(final SolicitudAprobada aprobada) {
        return aprobada.fechaHoraInicio() == null ? "" : aprobada.fechaHoraInicio().toString();
    }

    static final String NOMBRE_EVENTO = "SESION_MANUAL_CREADA";
}
