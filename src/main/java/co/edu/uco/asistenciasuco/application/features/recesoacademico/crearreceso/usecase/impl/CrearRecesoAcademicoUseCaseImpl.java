package co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.usecase.CrearRecesoAcademicoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.DestinatarioReceso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.RecesoCreado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * DEC-081: registra un dia de receso academico. La DB valida (fecha dentro del periodo y futura, no festivo, no repetida, periodo abierto,
 * solo el ADMINISTRADOR de la institucion) y cancela al instante las sesiones de ese dia. Despues del commit, y solo si se cancelo alguna:
 * <ul>
 *   <li>UN correo por persona afectada (estudiante o docente) con el resumen de sus clases canceladas ({@code RECESO_CLASES_CANCELADAS_*});
 *       las clases van en UNA linea separadas por {@code "; "}, en la hora institucional (America/Bogota).</li>
 *   <li>El evento {@code SESION_CANCELADA} (tipo {@code RECESO}) de cada grupo afectado, para refrescar los calendarios abiertos.</li>
 * </ul>
 * Los avisos son de mejor esfuerzo: un fallo se registra (WARN) y no revierte el receso ni cambia la respuesta.
 */
public final class CrearRecesoAcademicoUseCaseImpl implements CrearRecesoAcademicoUseCase {

    public static final String EVENTO_SESION_CANCELADA = "SESION_CANCELADA";
    /** Valor de {@code tipo} del evento {@code SESION_CANCELADA} cuando la cancelacion viene de un receso academico. */
    public static final String TIPO_CANCELACION_RECESO = "RECESO";
    static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SEPARADOR_CLASES = "; ";
    private static final Logger LOGGER = LoggerFactory.getLogger(CrearRecesoAcademicoUseCaseImpl.class);

    private final RecesoAcademicoRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public CrearRecesoAcademicoUseCaseImpl(
            final RecesoAcademicoRepositoryPort repositorio,
            final NotificacionPort notificaciones,
            final RealtimePublisherPort realtime
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "RecesoAcademicoRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para crear el receso academico es obligatorio.");
        if (comando.idPeriodo() == null) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069, "idPeriodo");
        }
        if (comando.fecha() == null) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069, "fecha");
        }
        if (comando.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_016);
        }
        // La longitud y que no este vacio (REC_008) los decide la DB; aqui solo se normaliza el espacio sobrante.
        final String motivo = comando.motivo() == null ? "" : comando.motivo().strip();

        final RecesoCreado creado = repositorio.crear(UUID.randomUUID(), comando.idPeriodo(), comando.fecha(), motivo,
                comando.usuarioEjecutor());

        int avisos = 0;
        int grupos = 0;
        if (creado.sesionesCanceladas() > 0) {
            // Una fila por (persona, sesion cancelada): de ella salen los correos (por persona) y los eventos (por grupo).
            final List<DestinatarioReceso> filas = consultarDestinatarios(creado.idReceso(), comando.usuarioEjecutor());
            avisos = avisar(creado.idReceso(), filas, comando.fecha(), motivo);
            grupos = publicarEventos(filas);
        }
        return new Resultado(creado.idReceso(), comando.fecha(), motivo, creado.sesionesCanceladas(), creado.sesionesConAsistencia(),
                creado.mensajeUsuario(), avisos, grupos);
    }

    /** Los destinatarios son de mejor esfuerzo: si la consulta falla se registra y el receso queda registrado sin avisos. */
    private List<DestinatarioReceso> consultarDestinatarios(final UUID idReceso, final UUID usuarioEjecutor) {
        try {
            return repositorio.consultarDestinatarios(idReceso, usuarioEjecutor);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los destinatarios del aviso del receso academico {}: {}", idReceso,
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    /** UN correo por persona y rol (estudiante o docente) con el resumen de sus clases canceladas; devuelve los correos encolados. */
    private int avisar(final UUID idReceso, final List<DestinatarioReceso> filas, final LocalDate fecha, final String motivo) {
        int encolados = 0;
        for (final List<DestinatarioReceso> clasesDeLaPersona : agruparPorPersona(filas).values()) {
            final DestinatarioReceso persona = clasesDeLaPersona.getFirst();
            try {
                notificaciones.notificar(construirAviso(persona, clasesDeLaPersona, fecha, motivo));
                encolados++;
            } catch (final RuntimeException exception) {
                LOGGER.warn("No fue posible avisar el receso academico {} a {} ({}): {}", idReceso, persona.idUsuario(),
                        persona.rolDestinatario(), exception.getClass().getSimpleName());
            }
        }
        return encolados;
    }

    /** Agrupa las filas (persona, sesion) por persona y rol, sin repetir una sesion, en el orden en que llegan. */
    private static Map<String, List<DestinatarioReceso>> agruparPorPersona(final List<DestinatarioReceso> filas) {
        final Map<String, List<DestinatarioReceso>> porPersona = new LinkedHashMap<>();
        final Set<String> vistas = new HashSet<>();
        for (final DestinatarioReceso fila : filas) {
            if (fila.idUsuario() == null) {
                continue;
            }
            final String persona = fila.rolDestinatario() + ":" + fila.idUsuario();
            if (vistas.add(persona + ":" + fila.idSesion())) {
                porPersona.computeIfAbsent(persona, ignorada -> new ArrayList<>()).add(fila);
            }
        }
        return porPersona;
    }

    private static Notificacion construirAviso(final DestinatarioReceso persona, final List<DestinatarioReceso> clasesDeLaPersona,
                                               final LocalDate fecha, final String motivo) {
        final boolean docente = DestinatarioReceso.ROL_DOCENTE.equals(persona.rolDestinatario());
        final String fechaTexto = fecha.format(FORMATO_FECHA);
        final String nombre = valor(persona.nombreCompleto());
        final String clases = clasesDeLaPersona.stream()
                .sorted(Comparator.comparing(DestinatarioReceso::fechaHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CrearRecesoAcademicoUseCaseImpl::describirClase)
                .collect(java.util.stream.Collectors.joining(SEPARADOR_CLASES));
        final Map<String, String> datos = new HashMap<>();
        datos.put(docente ? "docente" : "estudiante", nombre);
        datos.put("fecha", fechaTexto);
        datos.put("motivo", motivo);
        datos.put("clases", clases);
        final String asunto = "Clases canceladas por receso académico (" + fechaTexto + ")";
        final String mensaje = "Hola " + nombre + ". La institución declaró receso académico el " + fechaTexto + ". Motivo: " + motivo
                + (docente ? ". Estas clases suyas quedaron canceladas y no requieren toma de asistencia: "
                : ". Estas clases quedaron canceladas y no cuentan como inasistencia: ") + clases + ".";
        return new Notificacion(
                docente ? Notificacion.TIPO_RECESO_CLASES_CANCELADAS_DOCENTE : Notificacion.TIPO_RECESO_CLASES_CANCELADAS_ESTUDIANTE,
                asunto,
                mensaje,
                List.of(new Notificacion.Destinatario(persona.rolDestinatario(), persona.idUsuario(), persona.nombreCompleto(),
                        persona.correo())),
                datos);
    }

    /** {@code Asignatura, G01, 08:00 a 10:00} en la hora institucional. */
    private static String describirClase(final DestinatarioReceso clase) {
        return valor(clase.nombreAsignatura()) + ", " + valor(clase.nombreGrupo()) + ", " + hora(clase.fechaHoraInicio()) + " a "
                + hora(clase.fechaHoraFin());
    }

    private static String hora(final LocalDateTime utc) {
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA_INSTITUCIONAL).format(FORMATO_HORA);
    }

    /** UN evento {@code SESION_CANCELADA} por grupo afectado (con una de sus sesiones de ese dia); devuelve los grupos publicados. */
    private int publicarEventos(final List<DestinatarioReceso> filas) {
        final Map<UUID, DestinatarioReceso> primeraPorGrupo = new LinkedHashMap<>();
        for (final DestinatarioReceso fila : filas) {
            if (fila.idGrupo() != null && fila.idSesion() != null) {
                primeraPorGrupo.putIfAbsent(fila.idGrupo(), fila);
            }
        }
        int publicados = 0;
        for (final DestinatarioReceso sesion : primeraPorGrupo.values()) {
            final Map<String, Object> payload = new HashMap<>();
            payload.put("grupo", sesion.idGrupo().toString());
            payload.put("sesion", sesion.idSesion().toString());
            payload.put("tipo", TIPO_CANCELACION_RECESO);
            if (sesion.fechaHoraInicio() != null) {
                payload.put("fechaHoraInicioUtc", sesion.fechaHoraInicio().toInstant(ZoneOffset.UTC).toString());
            }
            try {
                realtime.publish(RealtimeEvent.of(EVENTO_SESION_CANCELADA, payload));
                publicados++;
            } catch (final RuntimeException exception) {
                LOGGER.warn("No fue posible publicar el evento del receso academico en el grupo {}: {}", sesion.idGrupo(),
                        exception.getClass().getSimpleName());
            }
        }
        return publicados;
    }

    private static String valor(final String texto) {
        return texto == null ? "" : texto;
    }
}
