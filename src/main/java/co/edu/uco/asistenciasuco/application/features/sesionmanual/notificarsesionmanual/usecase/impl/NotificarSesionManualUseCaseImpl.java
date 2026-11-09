package co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): aviso inmediato de una sesion creada manualmente por el docente. Localiza la sesion recien creada
 * (grupo + nombre + horas tal como se guardaron; la de mayor numero si hubiera varias), consulta en la DB los destinatarios
 * ({@code usp_consultar_destinatarios_sesion_manual}: estudiantes activos y coordinador(es) del programa), encola un aviso por
 * destinatario (outbox, plantillas {@code SESION_MANUAL_CREADA_*}) y publica el evento {@code SESION_MANUAL_CREADA}.
 *
 * <p>Todo es de mejor esfuerzo y ocurre despues del commit de la creacion: ningun fallo aqui revierte la sesion ni cambia la
 * respuesta; se registra y se continua.</p>
 */
public final class NotificarSesionManualUseCaseImpl implements NotificarSesionManualUseCase {

    public static final String EVENTO_SESION_MANUAL_CREADA = "SESION_MANUAL_CREADA";
    static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
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

        int estudiantes = 0;
        int coordinadores = 0;
        for (final DestinatarioSesionManual destinatario : destinatarios) {
            final Optional<Notificacion> aviso = construirAviso(sesionId, destinatario);
            if (aviso.isPresent() && encolar(sesionId, aviso.get())) {
                if (DestinatarioSesionManual.TIPO_ESTUDIANTE.equals(destinatario.tipo())) {
                    estudiantes++;
                } else {
                    coordinadores++;
                }
            }
        }
        final boolean publicado = publicarEvento(sesion.get(), destinatarios);
        return new Resultado(sesionId, estudiantes, coordinadores, publicado);
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
            return esperado == guardado;
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

    private boolean encolar(final UUID sesionId, final Notificacion aviso) {
        try {
            notificaciones.notificar(aviso);
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la sesion manual {} ({}): {}", sesionId, aviso.tipo(), exception.getClass().getSimpleName());
            return false;
        }
    }

    private static Optional<Notificacion> construirAviso(final UUID sesionId, final DestinatarioSesionManual d) {
        final boolean esEstudiante = DestinatarioSesionManual.TIPO_ESTUDIANTE.equals(d.tipo());
        final boolean esCoordinador = DestinatarioSesionManual.TIPO_COORDINADOR.equals(d.tipo());
        if (!esEstudiante && !esCoordinador) {
            return Optional.empty();
        }
        final String fecha = formatear(d.fechaHoraInicio(), FORMATO_FECHA);
        final String horaInicio = formatear(d.fechaHoraInicio(), FORMATO_HORA);
        final String horaFin = formatear(d.fechaHoraFin(), FORMATO_HORA);
        final String asignatura = valor(d.nombreAsignatura());
        final String grupo = valor(d.nombreGrupo());
        final String docente = valor(d.nombreDocente());
        final String nombre = valor(d.nombreCompleto());
        final Map<String, String> datos = new HashMap<>();
        datos.put(esEstudiante ? "estudiante" : "coordinador", nombre);
        datos.put("asignatura", asignatura);
        datos.put("grupo", grupo);
        datos.put("fecha", fecha);
        datos.put("horaInicio", horaInicio);
        datos.put("horaFin", horaFin);
        datos.put("docente", docente);
        datos.put("idSesion", sesionId.toString());
        final String ventana = " del grupo " + grupo + " para el " + fecha + " de " + horaInicio + " a " + horaFin + ".";
        final String asunto;
        final String mensaje;
        if (esEstudiante) {
            asunto = "Nueva sesión de clase: " + asignatura + " (" + fecha + ")";
            mensaje = "Hola " + nombre + ". El docente " + docente + " programó una sesión adicional de " + asignatura + ventana;
        } else {
            asunto = "Sesión creada manualmente: " + asignatura + " (" + fecha + ")";
            mensaje = "Hola " + nombre + ". El docente " + docente + " creó manualmente una sesión de " + asignatura + ventana
                    + " Si no corresponde, puede cancelarla desde su vista de sesiones.";
        }
        return Optional.of(new Notificacion(
                esEstudiante ? Notificacion.TIPO_SESION_MANUAL_CREADA_ESTUDIANTE : Notificacion.TIPO_SESION_MANUAL_CREADA_COORDINADOR,
                asunto, mensaje,
                List.of(new Notificacion.Destinatario(d.tipo(), d.idUsuario(), d.nombreCompleto(), d.correo())),
                datos));
    }

    private boolean publicarEvento(final SesionProcedenciaRepositoryProjection sesion, final List<DestinatarioSesionManual> destinatarios) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", sesion.grupo().toString());
        payload.put("sesion", sesion.sesion().toString());
        payload.put("fechaHoraInicio", fechaHoraInicio(sesion));
        payload.put("docente", destinatarios.stream().map(DestinatarioSesionManual::nombreDocente).filter(Objects::nonNull)
                .findFirst().orElse(""));
        try {
            realtime.publish(RealtimeEvent.of(EVENTO_SESION_MANUAL_CREADA, payload));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento de la sesion manual {}: {}", sesion.sesion(), exception.getClass().getSimpleName());
            return false;
        }
    }

    /** ISO-8601 con {@code Z} solo si el horario esta confirmado en UTC (v2); si no, el valor local sin zona de v1. */
    private static String fechaHoraInicio(final SesionProcedenciaRepositoryProjection sesion) {
        final boolean confirmada = EstadoHorarioSesion.desdeProcedencia(sesion.procedenciaTemporal()) == EstadoHorarioSesion.CONFIRMADA;
        return confirmada ? sesion.fechaHoraInicio().toInstant(ZoneOffset.UTC).toString() : sesion.fechaHoraInicio().toString();
    }

    /** La DB guarda la sesion en UTC; los textos del aviso se muestran en la hora institucional. */
    private static String formatear(final LocalDateTime utc, final DateTimeFormatter formato) {
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA_INSTITUCIONAL).format(formato);
    }

    private static String valor(final String texto) {
        return texto == null ? "" : texto;
    }
}
