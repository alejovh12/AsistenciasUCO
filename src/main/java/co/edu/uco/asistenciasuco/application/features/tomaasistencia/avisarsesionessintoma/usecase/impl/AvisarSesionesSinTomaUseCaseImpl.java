package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarsesionessintoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarsesionessintoma.usecase.AvisarSesionesSinTomaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.DestinatarioAvisoSinToma;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.SesionSinTomaPendiente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * NOTOMA-001: aviso a la coordinacion de la sesion sin toma de asistencia. Por cada sesion que la base de datos reporta como pendiente
 * (terminada, sin ninguna toma, con la ventana de registro vencida, dentro del atraso maximo y sin aviso previo) arma UNA
 * {@link Notificacion} por coordinador destinatario (rol COORDINADOR, sin repetir personas: la base de datos ya resuelve los coordinadores
 * de cada programa vigente del grupo) y luego registra la marca de sesion avisada con el total avisado. El correo solo informa: no
 * cambia ninguna asistencia (el coordinador es de solo lectura). Los textos se muestran en hora institucional (America/Bogota).
 *
 * <p>Mejor esfuerzo: un fallo al avisar a un coordinador se registra y se continua con los demas. Un fallo de la sesion (consulta de
 * destinatarios, todos los avisos fallidos o la marca) NO la marca: se reintenta en la siguiente pasada. Una sesion sin coordinadores
 * con correo (o que ya no aplica) se marca con total 0 para no consultarla otra vez. Nunca se registra el correo del destinatario.</p>
 */
public final class AvisarSesionesSinTomaUseCaseImpl implements AvisarSesionesSinTomaUseCase {

    static final String ROL_COORDINADOR = "COORDINADOR";
    static final String PLATAFORMA_SIN_ENLACE = "la plataforma de asistencia";
    static final String RUTA_SESIONES = "/app/coordinador/asistencia";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvisarSesionesSinTomaUseCaseImpl.class);

    private final AvisoSesionSinTomaRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final String urlPlataforma;

    public AvisarSesionesSinTomaUseCaseImpl(final AvisoSesionSinTomaRepositoryPort repositorio, final NotificacionPort notificaciones,
                                            final String urlPlataforma) {
        this.repositorio = Objects.requireNonNull(repositorio, "AvisoSesionSinTomaRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.urlPlataforma = urlPlataforma == null ? "" : urlPlataforma.strip();
    }

    @Override
    public int execute() {
        int avisadas = 0;
        for (final SesionSinTomaPendiente pendiente : repositorio.consultarSesionesPendientes()) {
            if (avisarSesion(pendiente.idSesion())) {
                avisadas++;
            }
        }
        return avisadas;
    }

    private boolean avisarSesion(final UUID sesionId) {
        final List<DestinatarioAvisoSinToma> destinatarios;
        try {
            destinatarios = repositorio.consultarDestinatarios(sesionId);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar los coordinadores de la sesion {} para avisar que no tuvo toma: {}", sesionId,
                    exception.getClass().getSimpleName());
            return false;
        }
        int avisados = 0;
        int fallidos = 0;
        for (final DestinatarioAvisoSinToma destinatario : destinatarios) {
            if (avisarCoordinador(sesionId, destinatario)) {
                avisados++;
            } else {
                fallidos++;
            }
        }
        if (avisados == 0 && fallidos > 0) {
            LOGGER.warn("Ningun aviso de sesion sin toma de la sesion {} pudo encolarse ({} coordinadores): se reintenta en la siguiente pasada.",
                    sesionId, fallidos);
            return false;
        }
        try {
            repositorio.registrarAviso(sesionId, avisados);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible marcar la sesion {} como avisada (sin toma): {}", sesionId, exception.getClass().getSimpleName());
            return false;
        }
        if (fallidos > 0) {
            LOGGER.warn("Aviso de sesion sin toma de la sesion {}: {} avisados y {} con fallo.", sesionId, avisados, fallidos);
        }
        return true;
    }

    private boolean avisarCoordinador(final UUID sesionId, final DestinatarioAvisoSinToma destinatario) {
        try {
            notificaciones.notificar(armarAviso(destinatario));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar a un coordinador que la sesion {} no tuvo toma: {}", sesionId, exception.getClass().getSimpleName());
            return false;
        }
    }

    private Notificacion armarAviso(final DestinatarioAvisoSinToma d) {
        final Map<String, String> datos = new LinkedHashMap<>();
        datos.put("coordinador", AvisoSesionSupport.valor(d.nombreCoordinador()));
        datos.put("docente", AvisoSesionSupport.valor(d.nombreDocente()));
        datos.put("asignatura", AvisoSesionSupport.valor(d.nombreAsignatura()));
        datos.put("grupo", AvisoSesionSupport.valor(d.nombreGrupo()));
        datos.put("fecha", AvisoSesionSupport.fecha(local(d.fechaHoraInicioUtc())));
        datos.put("horaInicio", AvisoSesionSupport.hora(local(d.fechaHoraInicioUtc())));
        datos.put("horaFin", AvisoSesionSupport.hora(local(d.fechaHoraFinUtc())));
        datos.put("enlaceSesiones", enlaceSesiones());
        datos.put("idSesion", d.idSesion().toString());
        final String asunto = "Sesión sin toma de asistencia: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")";
        final String mensaje = "Hola " + datos.get("coordinador") + ". El docente " + datos.get("docente") + " no tomó asistencia en la sesión de "
                + datos.get("asignatura") + " del grupo " + datos.get("grupo") + " del " + datos.get("fecha") + " de " + datos.get("horaInicio")
                + " a " + datos.get("horaFin") + ". Este aviso es solo informativo; puede revisar el grupo en " + datos.get("enlaceSesiones") + ".";
        final Notificacion.Destinatario coordinador = new Notificacion.Destinatario(
                ROL_COORDINADOR, d.idUsuarioCoordinador(), d.nombreCoordinador(), d.correoCoordinador());
        return new Notificacion(Notificacion.TIPO_SESION_SIN_TOMA_COORDINADOR, asunto, mensaje, List.of(coordinador), datos);
    }

    private String enlaceSesiones() {
        if (urlPlataforma.isEmpty()) {
            return PLATAFORMA_SIN_ENLACE;
        }
        final String base = urlPlataforma.endsWith("/") ? urlPlataforma.substring(0, urlPlataforma.length() - 1) : urlPlataforma;
        return base + RUTA_SESIONES;
    }

    private static LocalDateTime local(final Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
