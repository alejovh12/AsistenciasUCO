package co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.usecase.CancelarSesionCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CULM-001 / DEC-064 (HU047): el coordinador del programa cancela una sesion creada manualmente por el docente. El ambito del
 * coordinador ({@code SEC_002}), que la sesion sea manual ({@code SES_018}), el motivo del catalogo ({@code SES_019}), la
 * observacion ({@code SES_021}) y las reglas de cancelacion ({@code SES_011}, {@code SES_012}, {@code SES_016}) las valida la DB.
 *
 * <p>Tras el commit del SP (DEC-065 (3)) se encola UNA notificacion agrupada a los estudiantes y al docente (solo sesiones con
 * solicitud) y se publica el evento {@code SESION_CANCELADA} del grupo con {@code tipo = COORDINADOR} (sin motivo),
 * de mejor esfuerzo: un fallo del publicador se registra y no cambia la respuesta. La respuesta devuelve el motivo tal como quedo
 * guardado (nombre del motivo + observacion).</p>
 */
public final class CancelarSesionCoordinadorUseCaseImpl implements CancelarSesionCoordinadorUseCase {

    public static final String EVENTO_SESION_CANCELADA = "SESION_CANCELADA";
    /** Quien cancelo, en la respuesta y el evento (la DB guarda el tipo manual existente y el usuario ejecutor). */
    static final String TIPO_CANCELACION_COORDINADOR = "COORDINADOR";
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelarSesionCoordinadorUseCaseImpl.class);

    private final SesionProcedenciaQueryPort sesiones;
    private final SesionManualRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public CancelarSesionCoordinadorUseCaseImpl(
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
        Objects.requireNonNull(comando, "El comando para cancelar la sesion es obligatorio.");
        final SesionProcedenciaRepositoryProjection sesion = sesiones.consultarSesion(comando.sesionId())
                .orElseThrow(() -> new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE));

        final SesionCancelada cancelada = repositorio.cancelarPorCoordinador(
                comando.sesionId(), comando.codigoMotivo(), comando.observacion(), comando.usuarioEjecutor());

        avisarEstudiantesYDocente(comando, motivoGuardado(comando));
        publicarEvento(cancelada, sesion);
        return new Resultado(
                cancelada.idSesion(),
                cancelada.idGrupo(),
                EstadoSesion.CANCELADA,
                new CancelacionSesionDTO(TIPO_CANCELACION_COORDINADOR, motivoGuardado(comando), null,
                        cancelada.fechaCancelacionUtc() == null ? null : cancelada.fechaCancelacionUtc().toInstant(ZoneOffset.UTC)),
                cancelada.totalEstudiantesActivos());
    }

    /** El motivo compuesto por la DB (nombre del catalogo + observacion), leido de la cancelacion guardada. */
    private String motivoGuardado(final Comando comando) {
        return sesiones.consultarSesion(comando.sesionId())
                .map(SesionProcedenciaRepositoryProjection::cancelacion)
                .map(CancelacionSesionProjection::motivo)
                .orElse(null);
    }

    /**
     * DEC-065 (3): UNA notificacion {@code SESION_CANCELADA} con los estudiantes activos y el docente titular. Los destinatarios salen de
     * {@code usp_consultar_destinatarios_solicitud_sesion}, que solo cubre sesiones con solicitud; una sesion manual anterior a DEC-065
     * (sin solicitud) devuelve lista vacia y no se avisa por correo (el evento realtime se publica igual). Mejor esfuerzo.
     */
    private void avisarEstudiantesYDocente(final Comando comando, final String motivo) {
        try {
            final List<DestinatarioSesionManual> destinatarios = repositorio.consultarDestinatariosSolicitud(
                    comando.sesionId(), comando.usuarioEjecutor());
            final List<DestinatarioSesionManual> avisables = destinatarios.stream()
                    .filter(d -> DestinatarioSesionManual.TIPO_ESTUDIANTE.equals(d.tipo())
                            || DestinatarioSesionManual.TIPO_DOCENTE.equals(d.tipo()))
                    .toList();
            if (avisables.isEmpty()) {
                return;
            }
            final Map<String, String> datos = AvisoSesionSupport.datosBase(comando.sesionId(), avisables.getFirst());
            datos.put("estudiante", AvisoSesionSupport.SALUDO_ESTUDIANTES_Y_DOCENTE);
            datos.put("motivo", AvisoSesionSupport.valor(motivo));
            notificaciones.notificar(new Notificacion(
                    Notificacion.TIPO_SESION_CANCELADA,
                    "Clase cancelada: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")",
                    "Hola " + AvisoSesionSupport.SALUDO_ESTUDIANTES_Y_DOCENTE + ". " + AvisoSesionSupport.COORDINADOR_DEL_PROGRAMA
                            + " canceló la clase de " + datos.get("asignatura") + AvisoSesionSupport.ventana(datos).replaceAll("\\.$", "")
                            + ". Motivo: " + AvisoSesionSupport.valor(motivo) + ".",
                    AvisoSesionSupport.comoDestinatarios(avisables), datos));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la cancelacion de la sesion {}: {}", comando.sesionId(), exception.getClass().getSimpleName());
        }
    }

    private void publicarEvento(final SesionCancelada cancelada, final SesionProcedenciaRepositoryProjection sesion) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("grupo", cancelada.idGrupo().toString());
        payload.put("sesion", cancelada.idSesion().toString());
        payload.put("tipo", TIPO_CANCELACION_COORDINADOR);
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
}
