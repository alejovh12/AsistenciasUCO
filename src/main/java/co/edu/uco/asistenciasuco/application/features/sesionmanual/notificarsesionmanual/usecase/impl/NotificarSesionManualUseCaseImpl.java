package co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.NotificarSesionManualUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047) y DEC-065 (2)(3): aviso inmediato de una sesion creada por el docente. Localiza la sesion recien creada
 * (grupo + nombre + horas tal como se guardaron; la de mayor numero si hubiera varias) y consulta en la DB los destinatarios
 * ({@code usp_consultar_destinatarios_sesion_manual}: estudiantes activos, coordinador(es) del programa y el docente).
 * <ul>
 *   <li>Sesion SIN solicitud (coordinador, administrador, generador): UNA notificacion a todos los estudiantes y UNA a todos los
 *       coordinadores (plantillas {@code SESION_MANUAL_CREADA_*}) y evento {@code SESION_MANUAL_CREADA}.</li>
 *   <li>Sesion PENDIENTE de aprobacion (la crea un docente): solo UNA notificacion {@code SESION_SOLICITUD_CREADA_COORDINADOR} a
 *       los coordinadores y evento {@code SESION_SOLICITUD_CREADA}; los estudiantes no se avisan.</li>
 * </ul>
 *
 * <p>Todo es de mejor esfuerzo y ocurre despues del commit de la creacion: ningun fallo aqui revierte la sesion ni cambia la
 * respuesta; se registra y se continua.</p>
 */
public final class NotificarSesionManualUseCaseImpl implements NotificarSesionManualUseCase {

    public static final String EVENTO_SESION_MANUAL_CREADA = "SESION_MANUAL_CREADA";
    /** DEC-065 (2): la sesion del docente quedo como solicitud pendiente de aprobacion (audiencia: ambito del grupo + docente). */
    public static final String EVENTO_SESION_SOLICITUD_CREADA = "SESION_SOLICITUD_CREADA";
    static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificarSesionManualUseCaseImpl.class);

    private final SesionProcedenciaQueryPort sesiones;
    private final SesionManualRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public NotificarSesionManualUseCaseImpl(
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
        Objects.requireNonNull(comando, "El comando para avisar la sesion manual es obligatorio.");
        final Optional<SesionProcedenciaRepositoryProjection> sesion = localizar(comando);
        if (sesion.isEmpty()) {
            return Resultado.sinSesion();
        }
        final UUID sesionId = sesion.get().sesion();
        final List<DestinatarioSesionManual> destinatarios = consultarDestinatarios(sesionId, comando);

        if (ESTADO_PENDIENTE.equals(sesion.get().estadoSolicitud())) {
            return avisarSolicitud(sesion.get(), destinatarios);
        }
        final List<DestinatarioSesionManual> estudiantes = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_ESTUDIANTE);
        final List<DestinatarioSesionManual> coordinadores = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_COORDINADOR);
        final int avisadosEstudiantes = encolar(sesionId, AvisoSesionSupport.avisoSesionCreada(sesionId, estudiantes, true)) ? estudiantes.size() : 0;
        final int avisadosCoordinadores = encolar(sesionId, AvisoSesionSupport.avisoSesionCreada(sesionId, coordinadores, false)) ? coordinadores.size() : 0;
        final boolean publicado = publicarEvento(EVENTO_SESION_MANUAL_CREADA, sesion.get(), destinatarios);
        return new Resultado(sesionId, avisadosEstudiantes, avisadosCoordinadores, publicado);
    }

    /**
     * DEC-065 (2): la sesion del docente nace como solicitud pendiente. Solo se avisa al coordinador (UNA notificacion con todos los
     * coordinadores del programa) y se publica {@code SESION_SOLICITUD_CREADA}; los estudiantes no se enteran hasta la aprobacion.
     */
    private Resultado avisarSolicitud(final SesionProcedenciaRepositoryProjection sesion, final List<DestinatarioSesionManual> destinatarios) {
        final List<DestinatarioSesionManual> coordinadores = AvisoSesionSupport.deTipo(destinatarios, DestinatarioSesionManual.TIPO_COORDINADOR);
        final int avisados = encolar(sesion.sesion(), avisoSolicitud(sesion.sesion(), coordinadores)) ? coordinadores.size() : 0;
        final boolean publicado = publicarEvento(EVENTO_SESION_SOLICITUD_CREADA, sesion, destinatarios);
        return new Resultado(sesion.sesion(), 0, avisados, publicado);
    }

    private Optional<SesionProcedenciaRepositoryProjection> localizar(final Comando comando) {
        try {
            final String nombre = comando.nombre() == null ? null : comando.nombre().strip();
            return sesiones.consultarSesionesPorGrupo(comando.grupoId()).stream()
                    .filter(s -> Objects.equals(nombre, s.nombre() == null ? null : s.nombre().strip()))
                    .filter(s -> mismoInstante(comando.fechaHoraInicio(), s.fechaHoraInicio()))
                    .filter(s -> mismoInstante(comando.fechaHoraFin(), s.fechaHoraFin()))
                    .max(Comparator.comparing(s -> s.numero() == null ? Integer.MIN_VALUE : s.numero()));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible localizar la sesion manual creada en el grupo {}: {}", comando.grupoId(),
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static boolean mismoInstante(final LocalDateTime esperado, final LocalDateTime guardado) {
        if (esperado == null || guardado == null) {
            return esperado == null && guardado == null; // Sonar S8696: sin == entre tipos de valor
        }
        return esperado.truncatedTo(ChronoUnit.MICROS).equals(guardado.truncatedTo(ChronoUnit.MICROS));
    }

    private List<DestinatarioSesionManual> consultarDestinatarios(final UUID sesionId, final Comando comando) {
        try {
            return repositorio.consultarDestinatarios(sesionId, comando.docenteUsuarioId());
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los destinatarios del aviso de la sesion manual {}: {}", sesionId,
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    /** Encola el aviso agrupado; {@code Optional.empty()} (sin destinatarios) no es un fallo ni se cuenta. */
    private boolean encolar(final UUID sesionId, final Optional<Notificacion> aviso) {
        if (aviso.isEmpty()) {
            return false;
        }
        try {
            notificaciones.notificar(aviso.get());
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la sesion manual {} ({}): {}", sesionId, aviso.get().tipo(), exception.getClass().getSimpleName());
            return false;
        }
    }

    private static Optional<Notificacion> avisoSolicitud(final UUID sesionId, final List<DestinatarioSesionManual> coordinadores) {
        if (coordinadores.isEmpty()) {
            return Optional.empty();
        }
        final Map<String, String> datos = AvisoSesionSupport.datosBase(sesionId, coordinadores.getFirst());
        datos.put("coordinador", AvisoSesionSupport.SALUDO_COORDINADORES);
        return Optional.of(new Notificacion(
                Notificacion.TIPO_SESION_SOLICITUD_CREADA_COORDINADOR,
                "Solicitud de sesión extraordinaria: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")",
                "Hola " + AvisoSesionSupport.SALUDO_COORDINADORES + ". El docente " + datos.get("docente")
                        + " solicitó una sesión extraordinaria de " + datos.get("asignatura") + AvisoSesionSupport.ventana(datos)
                        + " La sesión queda pendiente de su aprobación; puede aprobarla o rechazarla desde su vista de solicitudes.",
                AvisoSesionSupport.comoDestinatarios(coordinadores), datos));
    }

    private boolean publicarEvento(final String tipo, final SesionProcedenciaRepositoryProjection sesion,
                                   final List<DestinatarioSesionManual> destinatarios) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", sesion.grupo().toString());
        payload.put("sesion", sesion.sesion().toString());
        payload.put("fechaHoraInicio", AvisoSesionSupport.fechaHoraInicio(sesion));
        payload.put("docente", destinatarios.stream().map(DestinatarioSesionManual::nombreDocente).filter(Objects::nonNull)
                .findFirst().orElse(""));
        try {
            realtime.publish(RealtimeEvent.of(tipo, payload));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento {} de la sesion {}: {}", tipo, sesion.sesion(), exception.getClass().getSimpleName());
            return false;
        }
    }
}
