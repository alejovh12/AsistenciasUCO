package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadAlerta;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoEvaluacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C, contrato 3.4-3.6): ejecuta el motor de alertas de un grupo y avisa lo que la DB reporto como novedad.
 * Alerta principal -> correo al estudiante (plantilla del tipo); toda alerta nueva -> evento {@code ALERTA_INASISTENCIA} (destinatario
 * el estudiante); revision creada -> correo al docente titular y evento {@code REVISION_CANCELACION_CREADA} (destinatario el docente).
 * Mejor esfuerzo: cualquier fallo se registra (sin datos personales) y no se propaga. Los destinatarios viajan como metadato del evento, nunca
 * en el payload.
 */
public final class AvisoAlertasInasistenciaService implements EvaluadorAlertasInasistencia {

    public static final String EVENTO_ALERTA_INASISTENCIA = "ALERTA_INASISTENCIA";
    public static final String EVENTO_REVISION_CREADA = "REVISION_CANCELACION_CREADA";
    static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    static final String ROL_DOCENTE = "DOCENTE";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvisoAlertasInasistenciaService.class);

    private final AlertaInasistenciaRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final RealtimePublisherPort realtime;

    public AvisoAlertasInasistenciaService(
            final AlertaInasistenciaRepositoryPort repositorio,
            final NotificacionPort notificaciones,
            final RealtimePublisherPort realtime
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "AlertaInasistenciaRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public void evaluarYAvisar(final UUID grupoId, final UUID estudianteGrupoId) {
        try {
            final ResultadoEvaluacion resultado = repositorio.evaluar(grupoId, estudianteGrupoId);
            if (resultado == null || resultado.novedades() == null) {
                return;
            }
            for (final NovedadAlerta novedad : resultado.novedades()) {
                avisar(novedad);
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible evaluar las alertas de inasistencia del grupo {}: {}", grupoId, exception.getClass().getSimpleName());
        }
    }

    @Override
    public void evaluarPeriodoYAvisar(final UUID periodoId) {
        final List<UUID> grupos;
        try {
            grupos = repositorio.consultarGruposPendientes(null, periodoId);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible listar los grupos del periodo {} para evaluar alertas: {}", periodoId, exception.getClass().getSimpleName());
            return;
        }
        for (final UUID grupo : grupos) {
            evaluarYAvisar(grupo, null);
        }
    }

    private void avisar(final NovedadAlerta n) {
        if (n.esPrincipal() && n.estudiante() != null) {
            ejecutarSeguro("aviso de alerta " + n.idAlerta(), () -> notificaciones.notificar(avisoEstudiante(n)));
        }
        if (n.estudiante() != null && n.estudiante().idUsuario() != null) {
            final Map<String, Object> payload = new HashMap<>();
            payload.put("grupo", n.idGrupo().toString());
            payload.put("alerta", n.idAlerta().toString());
            payload.put("estudianteGrupo", n.idEstudianteGrupo().toString());
            payload.put("tipo", n.codigoTipo());
            payload.put("porcentajeInasistencia", n.porcentaje());
            payload.put("umbral", n.umbral());
            ejecutarSeguro("evento de alerta " + n.idAlerta(),
                    () -> realtime.publish(RealtimeEvent.paraDestinatarios(EVENTO_ALERTA_INASISTENCIA, payload, Set.of(n.estudiante().idUsuario()))));
        }
        if (n.idRevisionCreada() != null && n.docente() != null) {
            ejecutarSeguro("aviso de revision " + n.idRevisionCreada(), () -> notificaciones.notificar(avisoDocente(n)));
            if (n.docente().idUsuario() != null) {
                final Map<String, Object> payload = new HashMap<>();
                payload.put("grupo", n.idGrupo().toString());
                payload.put("revision", n.idRevisionCreada().toString());
                payload.put("estudianteGrupo", n.idEstudianteGrupo().toString());
                payload.put("estado", "PENDIENTE_DOCENTE");
                ejecutarSeguro("evento de revision " + n.idRevisionCreada(),
                        () -> realtime.publish(RealtimeEvent.paraDestinatarios(EVENTO_REVISION_CREADA, payload, Set.of(n.docente().idUsuario()))));
            }
        }
    }

    private static Notificacion avisoEstudiante(final NovedadAlerta n) {
        final String grupo = texto(n.nombreGrupo()) + " de " + texto(n.nombreAsignatura());
        final Map<String, String> datos = new HashMap<>();
        datos.put("estudiante", texto(n.estudiante().nombreCompleto()));
        datos.put("grupo", grupo);
        datos.put("porcentaje", n.porcentaje() == null ? "" : n.porcentaje().toPlainString());
        datos.put("umbral", n.umbral() == null ? "" : n.umbral().toPlainString());
        final var limite = n.umbralPerdida() != null ? n.umbralPerdida() : n.umbral();
        datos.put("limite", limite == null ? "" : limite.toPlainString());
        datos.put("idAlerta", n.idAlerta().toString());
        final String asunto = "Alerta de inasistencia en " + grupo;
        final String mensaje = "Estimado(a) " + texto(n.estudiante().nombreCompleto()) + ", acumulas " + datos.get("porcentaje")
                + "% de inasistencia en " + grupo + " (umbral " + datos.get("umbral") + "%, límite " + datos.get("limite") + "%).";
        return new Notificacion(n.tipoPlantillaEstudiante(), asunto, mensaje,
                List.of(new Notificacion.Destinatario(ROL_ESTUDIANTE, n.estudiante().idUsuario(), n.estudiante().nombreCompleto(), n.estudiante().correo())),
                datos);
    }

    private static Notificacion avisoDocente(final NovedadAlerta n) {
        final String grupo = texto(n.nombreGrupo()) + " de " + texto(n.nombreAsignatura());
        final Map<String, String> datos = new HashMap<>();
        datos.put("docente", texto(n.docente().nombreCompleto()));
        datos.put("estudiante", texto(n.estudiante() == null ? null : n.estudiante().nombreCompleto()));
        datos.put("grupo", grupo);
        datos.put("porcentaje", n.porcentaje() == null ? "" : n.porcentaje().toPlainString());
        datos.put("limite", n.umbral() == null ? "" : n.umbral().toPlainString());
        datos.put("idRevision", n.idRevisionCreada().toString());
        final String asunto = "Revisión de cancelación pendiente: " + datos.get("estudiante") + " en " + grupo;
        final String mensaje = "Hola " + texto(n.docente().nombreCompleto()) + ". El estudiante " + datos.get("estudiante") + " alcanzó el "
                + datos.get("porcentaje") + "% de inasistencia en " + grupo + ". Decide el caso desde la planilla del grupo.";
        return new Notificacion(Notificacion.TIPO_REVISION_CANCELACION_PENDIENTE, asunto, mensaje,
                List.of(new Notificacion.Destinatario(ROL_DOCENTE, n.docente().idUsuario(), n.docente().nombreCompleto(), n.docente().correo())),
                datos);
    }

    private static void ejecutarSeguro(final String descripcion, final Runnable accion) {
        try {
            accion.run();
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el {}: {}", descripcion, exception.getClass().getSimpleName());
        }
    }

    private static String texto(final String valor) {
        return valor == null ? "" : valor;
    }
}
