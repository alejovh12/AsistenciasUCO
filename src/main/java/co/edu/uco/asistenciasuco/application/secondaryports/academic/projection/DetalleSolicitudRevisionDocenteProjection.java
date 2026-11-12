package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de una solicitud de revision de asistencia visible para el docente titular (HU036): datos de la
 * solicitud, estudiante, sesion, grupo y asignatura, causal de la asistencia ({@code asistio} y {@code RazonCausa}) y
 * metadatos del soporte adjunto. Causal y soporte son nulos si no existe detalle de asistencia o archivo adjunto.
 * Los instantes son UTC; {@code fechaHoraInicioSesionUtc} es nula si {@code procedenciaTemporalSesion} es nula.
 * CULM-001 D.2: {@code codigoCategoria}/{@code nombreCategoria} (nulos en reclamos anteriores a la categoria persistida) y
 * {@code soportes} (todos, en orden de adjuncion; las columnas de soporte unico siguen siendo las del primero).
 */
public record DetalleSolicitudRevisionDocenteProjection(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID idEstudiante,
        String nombreEstudiante,
        UUID idSesion,
        Integer numeroSesion,
        Instant fechaHoraInicioSesionUtc,
        String procedenciaTemporalSesion,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        Boolean asistio,
        String codigoRazonCausa,
        String nombreRazonCausa,
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc,
        String codigoCategoria,
        String nombreCategoria,
        List<SoporteReclamoProjection> soportes,
        UUID idToma,
        Integer numeroToma,
        Instant fechaPuntoControlUtc,
        String codigoResultadoAprobacion,
        String codigoRazonCausaToma,
        Integer minutosSesion,
        Integer minutosAsistidos
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public DetalleSolicitudRevisionDocenteProjection(
            final UUID idSolicitud,
            final String nombreSolicitud,
            final LocalDate fechaSolicitud,
            final String codigoEstado,
            final String nombreEstado,
            final String justificacionSolicitud,
            final String justificacionRespuesta,
            final UUID idEstudiante,
            final String nombreEstudiante,
            final UUID idSesion,
            final Integer numeroSesion,
            final Instant fechaHoraInicioSesionUtc,
            final String procedenciaTemporalSesion,
            final UUID idGrupo,
            final Integer codigoGrupo,
            final String nombreGrupo,
            final UUID idAsignatura,
            final String codigoAsignatura,
            final String nombreAsignatura,
            final Boolean asistio,
            final String codigoRazonCausa,
            final String nombreRazonCausa,
            final UUID idArchivoSoporte,
            final String nombreSoporte,
            final String tipoContenidoSoporte,
            final Long tamanioSoporteBytes,
            final Instant fechaRegistroSoporteUtc,
            final String codigoCategoria,
            final String nombreCategoria,
            final List<SoporteReclamoProjection> soportes
    ) {
        this(idSolicitud, nombreSolicitud, fechaSolicitud, codigoEstado, nombreEstado, justificacionSolicitud, justificacionRespuesta, idEstudiante, nombreEstudiante, idSesion, numeroSesion, fechaHoraInicioSesionUtc, procedenciaTemporalSesion, idGrupo, codigoGrupo, nombreGrupo, idAsignatura, codigoAsignatura, nombreAsignatura, asistio, codigoRazonCausa, nombreRazonCausa, idArchivoSoporte, nombreSoporte, tipoContenidoSoporte, tamanioSoporteBytes, fechaRegistroSoporteUtc, codigoCategoria, nombreCategoria, soportes, null, null, null, null, null, null, null);
    }
}
