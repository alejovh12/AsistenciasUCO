package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.common.AvisoSesionSupport;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.usecase.AvisarFaltasSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort.FaltaSesionAviso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort.SesionPendienteAviso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ATA D1 (HU175, ADDENDUM_ATA_ASISTENCIA_TOMAS_AVISOS C): aviso de falta por sesion. Por cada sesion que la base de datos reporta como
 * pendiente (terminada, con todas sus tomas cerradas, dentro del atraso maximo y sin aviso previo) arma UNA {@link Notificacion} por
 * estudiante con al menos una toma en SJC, con destinatario unico y rol ESTUDIANTE, y luego registra la marca de sesion avisada con el
 * total avisado.
 * <ul>
 *   <li>{@code detalleFaltas}: "falta general" si quedo SJC en todas las tomas; si no, las tomas en falta con su hora (punto de
 *       control) en hora institucional (America/Bogota, como {@link AvisoSesionSupport}).</li>
 *   <li>{@code fechaLimiteReclamo}: ultimo dia para reclamar (el SP entrega el instante exclusivo del plazo; se muestra el dia anterior).</li>
 *   <li>{@code enlaceReclamo}: enlace a la materia en la plataforma; sin URL configurada se nombra la plataforma sin enlace.</li>
 * </ul>
 *
 * <p>Mejor esfuerzo: un fallo al avisar a un estudiante se registra y se continua con los demas. Un fallo de la sesion (consulta de faltas,
 * todos los avisos fallidos o la marca) NO la marca: se reintenta en la siguiente pasada. Una sesion sin estudiantes con falta se marca con
 * total 0 para no consultarla otra vez. Nunca se registra el correo del destinatario.</p>
 */
public final class AvisarFaltasSesionUseCaseImpl implements AvisarFaltasSesionUseCase {

    static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    static final String FALTA_GENERAL = "falta general (no registró asistencia en ninguna toma)";
    static final String PLATAFORMA_SIN_ENLACE = "la plataforma de asistencia";
    static final String RUTA_MATERIA = "/app/estudiante/mis-materias/materias?materia=%s&grupo=%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvisarFaltasSesionUseCaseImpl.class);

    private final AvisoFaltaSesionRepositoryPort repositorio;
    private final NotificacionPort notificaciones;
    private final String urlPlataforma;

    public AvisarFaltasSesionUseCaseImpl(final AvisoFaltaSesionRepositoryPort repositorio, final NotificacionPort notificaciones,
                                         final String urlPlataforma) {
        this.repositorio = Objects.requireNonNull(repositorio, "AvisoFaltaSesionRepositoryPort es obligatorio.");
        this.notificaciones = Objects.requireNonNull(notificaciones, "NotificacionPort es obligatorio.");
        this.urlPlataforma = urlPlataforma == null ? "" : urlPlataforma.strip();
    }

    @Override
    public int execute() {
        int avisadas = 0;
        for (final SesionPendienteAviso pendiente : repositorio.consultarSesionesPendientes()) {
            if (avisarSesion(pendiente.idSesion())) {
                avisadas++;
            }
        }
        return avisadas;
    }

    private boolean avisarSesion(final UUID sesionId) {
        final List<FaltaSesionAviso> faltas;
        try {
            faltas = repositorio.consultarFaltas(sesionId);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar las faltas de la sesion {} para avisar: {}", sesionId, exception.getClass().getSimpleName());
            return false;
        }
        final Map<UUID, List<FaltaSesionAviso>> porEstudiante = faltas.stream()
                .collect(Collectors.groupingBy(FaltaSesionAviso::idUsuarioEstudiante, LinkedHashMap::new, Collectors.toList()));
        int avisados = 0;
        int fallidos = 0;
        for (final List<FaltaSesionAviso> deEstudiante : porEstudiante.values()) {
            if (avisarEstudiante(sesionId, deEstudiante)) {
                avisados++;
            } else {
                fallidos++;
            }
        }
        if (avisados == 0 && fallidos > 0) {
            LOGGER.warn("Ningun aviso de falta de la sesion {} pudo encolarse ({} estudiantes): se reintenta en la siguiente pasada.", sesionId, fallidos);
            return false;
        }
        try {
            repositorio.registrarAviso(sesionId, avisados);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible marcar la sesion {} como avisada: {}", sesionId, exception.getClass().getSimpleName());
            return false;
        }
        if (fallidos > 0) {
            LOGGER.warn("Aviso de falta de la sesion {}: {} avisados y {} con fallo.", sesionId, avisados, fallidos);
        }
        return true;
    }

    private boolean avisarEstudiante(final UUID sesionId, final List<FaltaSesionAviso> faltas) {
        try {
            notificaciones.notificar(armarAviso(faltas));
            return true;
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la falta de un estudiante en la sesion {}: {}", sesionId, exception.getClass().getSimpleName());
            return false;
        }
    }

    private Notificacion armarAviso(final List<FaltaSesionAviso> faltas) {
        final FaltaSesionAviso referencia = faltas.getFirst();
        final Map<String, String> datos = new LinkedHashMap<>();
        datos.put("estudiante", AvisoSesionSupport.valor(referencia.nombreEstudiante()));
        datos.put("asignatura", AvisoSesionSupport.valor(referencia.nombreAsignatura()));
        datos.put("grupo", AvisoSesionSupport.valor(referencia.nombreGrupo()));
        datos.put("fecha", AvisoSesionSupport.fecha(local(referencia.fechaHoraInicioUtc())));
        datos.put("horaInicio", AvisoSesionSupport.hora(local(referencia.fechaHoraInicioUtc())));
        datos.put("horaFin", AvisoSesionSupport.hora(local(referencia.fechaHoraFinUtc())));
        datos.put("detalleFaltas", detalleFaltas(faltas));
        datos.put("fechaLimiteReclamo", fechaLimite(referencia.fechaLimiteReclamoUtc()));
        datos.put("enlaceReclamo", enlaceReclamo(referencia));
        datos.put("idSesion", referencia.idSesion().toString());
        final String asunto = "Registro de inasistencia: " + datos.get("asignatura") + " (" + datos.get("fecha") + ")";
        final String mensaje = "Hola " + datos.get("estudiante") + ". En la sesión de " + datos.get("asignatura") + " del grupo "
                + datos.get("grupo") + " del " + datos.get("fecha") + " de " + datos.get("horaInicio") + " a " + datos.get("horaFin")
                + " quedó registrada inasistencia: " + datos.get("detalleFaltas") + ". Si consideras que hay un error, puedes radicar un reclamo por cada toma hasta el "
                + datos.get("fechaLimiteReclamo") + " desde " + datos.get("enlaceReclamo") + ".";
        final Notificacion.Destinatario destinatario = new Notificacion.Destinatario(
                ROL_ESTUDIANTE, referencia.idUsuarioEstudiante(), referencia.nombreEstudiante(), referencia.correoEstudiante());
        return new Notificacion(Notificacion.TIPO_FALTA_ASISTENCIA_SESION_ESTUDIANTE, asunto, mensaje, List.of(destinatario), datos);
    }

    /** "falta general ..." si todas las tomas quedaron en SJC; si no, "toma 1 (08:15), toma 3 (09:40)" en hora institucional. */
    static String detalleFaltas(final List<FaltaSesionAviso> faltas) {
        final FaltaSesionAviso referencia = faltas.getFirst();
        if (referencia.totalTomasEnFalta() >= referencia.totalTomasSesion()) {
            return FALTA_GENERAL;
        }
        return faltas.stream()
                .sorted(Comparator.comparingInt(FaltaSesionAviso::numeroToma))
                .map(f -> "toma " + f.numeroToma() + " (" + AvisoSesionSupport.hora(local(f.fechaPuntoControlUtc())) + ")")
                .collect(Collectors.joining(", "));
    }

    /** El SP entrega el instante EXCLUSIVO del plazo (medianoche local): el ultimo dia para reclamar es el anterior. */
    private static String fechaLimite(final Instant limiteExclusivoUtc) {
        return limiteExclusivoUtc == null ? "" : AvisoSesionSupport.fecha(local(limiteExclusivoUtc.minusSeconds(1)));
    }

    private String enlaceReclamo(final FaltaSesionAviso falta) {
        if (urlPlataforma.isEmpty()) {
            return PLATAFORMA_SIN_ENLACE;
        }
        final String base = urlPlataforma.endsWith("/") ? urlPlataforma.substring(0, urlPlataforma.length() - 1) : urlPlataforma;
        return base + RUTA_MATERIA.formatted(falta.idAsignatura(), falta.idGrupo());
    }

    private static LocalDateTime local(final Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
