package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud de revision propia del estudiante autenticado (HU010/HU011), con los datos minimos del soporte
 * adjunto (si existe). Misma semantica temporal que la bandeja docente: {@code fechaHoraInicioSesionUtc} es nula
 * cuando la hora de la sesion no tiene procedencia UTC verificable. CULM-001 D.2: {@code codigoCategoria}/{@code nombreCategoria}
 * (nulos en reclamos anteriores a la categoria persistida) y {@code soportes} (todos los soportes, en orden de adjuncion; las
 * columnas de soporte unico siguen siendo las del primero).
 */
public record SolicitudRevisionEstudianteProjection(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
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
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc,
        int totalRegistros,
        String codigoCategoria,
        String nombreCategoria,
        List<SoporteReclamoProjection> soportes,
        UUID idToma,
        Integer numeroToma,
        Instant fechaPuntoControlUtc,
        String codigoResultadoAprobacion
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public SolicitudRevisionEstudianteProjection(
            final UUID idSolicitud,
            final String nombreSolicitud,
            final LocalDate fechaSolicitud,
            final String codigoEstado,
            final String nombreEstado,
            final String justificacionSolicitud,
            final String justificacionRespuesta,
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
            final UUID idArchivoSoporte,
            final String nombreSoporte,
            final String tipoContenidoSoporte,
            final Long tamanioSoporteBytes,
            final Instant fechaRegistroSoporteUtc,
            final int totalRegistros,
            final String codigoCategoria,
            final String nombreCategoria,
            final List<SoporteReclamoProjection> soportes
    ) {
        this(idSolicitud, nombreSolicitud, fechaSolicitud, codigoEstado, nombreEstado, justificacionSolicitud, justificacionRespuesta, idSesion, numeroSesion, fechaHoraInicioSesionUtc, procedenciaTemporalSesion, idGrupo, codigoGrupo, nombreGrupo, idAsignatura, codigoAsignatura, nombreAsignatura, idArchivoSoporte, nombreSoporte, tipoContenidoSoporte, tamanioSoporteBytes, fechaRegistroSoporteUtc, totalRegistros, codigoCategoria, nombreCategoria, soportes, null, null, null, null);
    }
}
