package co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.usecase.CancelarSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.DestinatarioSesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CULM-001 B.1 (HU047): el docente titular cancela una sesion con un motivo. La titularidad se valida aqui (403) y de
 * nuevo en la DB, que ademas exige RBAC DOCENTE, la longitud del motivo ({@code SES_013}) y que la sesion no tenga
 * asistencia ({@code SES_012}/{@code SES_016}); una sesion ya cancelada responde {@code SES_011}.
 *
 * <p>Tras el commit del SP: (1) UN aviso {@code SESION_CANCELADA} con todos los estudiantes activos agrupados (DEC-065 (3); mejor
 * esfuerzo: un fallo se registra y no cambia la respuesta) y (2) el evento SSE {@code SESION_CANCELADA} del grupo, sin el motivo. Ante
 * cualquier error del SP no hay aviso ni evento.</p>
 */
public final class CancelarSesionUseCaseImpl implements CancelarSesionUseCase {

    public static final String EVENTO_SESION_CANCELADA = "SESION_CANCELADA";
    /** Tipo de cancelacion que crea el docente ({@code TipoCancelacionSesion.esAutomatica = 0}, contrato §3.2.1). */
    static final String TIPO_CANCELACION_DOCENTE = "DOCENTE";
    static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    /** Zona institucional de los textos del aviso (§3.7: {@code dd/MM/yyyy} en America/Bogota). */
    static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelarSesionUseCaseImpl.class);

    private final InstitutionalScopePort scope;
    private final SesionProcedenciaQueryPort sesiones;
    private final SesionCancelacionRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public CancelarSesionUseCaseImpl(
            final InstitutionalScopePort scope,
            final SesionProcedenciaQueryPort sesiones,
            final SesionCancelacionRepositoryPort repositorio,
            final NotificacionPort notificaciones,
            final RealtimePublisherPort realtime
    ) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.sesiones = Objects.requireNonNull(sesiones, "SesionProcedenciaQueryPort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "SesionCancelacionRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para cancelar la sesion es obligatorio.");
        final SesionProcedenciaRepositoryProjection sesion = sesiones.consultarSesion(comando.sesionId())
                .orElseThrow(() -> new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE));
        if (!scope.canDocenteAccessGrupo(comando.usuarioEjecutor(), sesion.grupo())) {
            throw new ForbiddenException(AsistenciaErrorCode.ERR_DOCENTE_SIN_TITULARIDAD_SESION);
        }
        final String motivo = comando.motivo() == null ? null : comando.motivo().strip();

        final SesionCancelada cancelada = repositorio.cancelar(comando.sesionId(), motivo, comando.usuarioEjecutor());

        final int notificados = avisarEstudiantes(comando, motivo);
        publicarEvento(cancelada, sesion);
        return new Resultado(
                cancelada.idSesion(),
                cancelada.idGrupo(),
                EstadoSesion.CANCELADA,
                new CancelacionSesionDTO(TIPO_CANCELACION_DOCENTE, motivo, null,
                        cancelada.fechaCancelacionUtc() == null ? null : cancelada.fechaCancelacionUtc().toInstant(ZoneOffset.UTC)),
                notificados);
    }

    /** DEC-065 (3): UNA notificacion con todos los estudiantes activos (el canal los reparte en bloques BCC). */
    private int avisarEstudiantes(final Comando comando, final String motivo) {
        final List<DestinatarioSesionCancelada> destinatarios;
        try {
            destinatarios = repositorio.consultarDestinatarios(comando.sesionId(), comando.usuarioEjecutor());
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los destinatarios del aviso de la sesion cancelada {}: {}",
                    comando.sesionId(), exception.getClass().getSimpleName());
            return 0;
        }
        if (destinatarios.isEmpty()) {
            return 0;
        }
        try {
            notificaciones.notificar(construirAviso(comando, destinatarios, motivo));
            return destinatarios.size();
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la cancelacion de la sesion {} a los estudiantes: {}",
                    comando.sesionId(), exception.getClass().getSimpleName());
            return 0;
        }
    }

    private static Notificacion construirAviso(final Comando comando, final List<DestinatarioSesionCancelada> destinatarios,
                                               final String motivo) {
        final DestinatarioSesionCancelada referencia = destinatarios.getFirst();
        final String fecha = formatear(referencia.fechaHoraInicio(), FORMATO_FECHA);
        final String horaInicio = formatear(referencia.fechaHoraInicio(), FORMATO_HORA);
        final String horaFin = formatear(referencia.fechaHoraFin(), FORMATO_HORA);
        final String motivoAviso = referencia.motivoCancelacion() == null ? motivo : referencia.motivoCancelacion();
        final Map<String, String> datos = new HashMap<>();
        datos.put("estudiante", AvisoSesionSupport.SALUDO_ESTUDIANTES);
        datos.put("asignatura", valor(referencia.nombreAsignatura()));
        datos.put("grupo", valor(referencia.nombreGrupo()));
        datos.put("fecha", fecha);
        datos.put("horaInicio", horaInicio);
        datos.put("horaFin", horaFin);
        datos.put("motivo", valor(motivoAviso));
        datos.put("docente", valor(referencia.nombreDocente()));
        datos.put("idSesion", comando.sesionId().toString());
        final String asunto = "Clase cancelada: " + valor(referencia.nombreAsignatura()) + " (" + fecha + ")";
        final String mensaje = "Hola " + AvisoSesionSupport.SALUDO_ESTUDIANTES + ". El docente " + valor(referencia.nombreDocente())
                + " canceló la clase de " + valor(referencia.nombreAsignatura()) + " del grupo " + valor(referencia.nombreGrupo())
                + " programada para el " + fecha + " de " + horaInicio + " a " + horaFin + ". Motivo: " + valor(motivoAviso) + ".";
        return new Notificacion(
                Notificacion.TIPO_SESION_CANCELADA,
                asunto,
                mensaje,
                destinatarios.stream()
                        .map(d -> new Notificacion.Destinatario(ROL_ESTUDIANTE, d.idUsuario(), d.nombreCompleto(), d.correo()))
                        .toList(),
                datos);
    }

    private void publicarEvento(final SesionCancelada cancelada, final SesionProcedenciaRepositoryProjection sesion) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", cancelada.idGrupo().toString());
        payload.put("sesion", cancelada.idSesion().toString());
        payload.put("tipo", TIPO_CANCELACION_DOCENTE);
        final boolean confirmada = EstadoHorarioSesion.desdeProcedencia(sesion.procedenciaTemporal()) == EstadoHorarioSesion.CONFIRMADA
                && sesion.fechaHoraInicio() != null;
        if (confirmada) {
            payload.put("fechaHoraInicioUtc", sesion.fechaHoraInicio().toInstant(ZoneOffset.UTC).toString());
        }
        try {
            realtime.publish(RealtimeEvent.of(EVENTO_SESION_CANCELADA, payload));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento de la sesion cancelada {}: {}", cancelada.idSesion(),
                    exception.getClass().getSimpleName());
        }
    }

    /** La DB guarda la sesion en UTC; los textos del aviso se muestran en la hora institucional. */
    private static String formatear(final LocalDateTime utc, final DateTimeFormatter formato) {
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA_INSTITUCIONAL).format(formato);
    }

    private static String valor(final String texto) {
        return texto == null ? "" : texto;
    }
}
