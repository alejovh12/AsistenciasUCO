package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.domain.SoporteReclamoDomain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Solicitud de revision propia del estudiante (los instantes son UTC; nulos si no hay procedencia verificable). */
public record ReclamoEstudianteDomain(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
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
        String codigoCategoria,
        String nombreCategoria,
        List<SoporteReclamoDomain> soportes,
        UUID idToma,
        Integer numeroToma,
        Instant fechaPuntoControlUtc,
        String codigoResultadoAprobacion
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public ReclamoEstudianteDomain(
            final UUID idSolicitud,
            final String nombreSolicitud,
            final LocalDate fechaSolicitud,
            final String codigoEstado,
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
            final String codigoCategoria,
            final String nombreCategoria,
            final List<SoporteReclamoDomain> soportes
    ) {
        this(idSolicitud, nombreSolicitud, fechaSolicitud, codigoEstado, justificacionSolicitud, justificacionRespuesta, idSesion, numeroSesion, fechaHoraInicioSesionUtc, procedenciaTemporalSesion, idGrupo, codigoGrupo, nombreGrupo, idAsignatura, codigoAsignatura, nombreAsignatura, idArchivoSoporte, nombreSoporte, tipoContenidoSoporte, tamanioSoporteBytes, fechaRegistroSoporteUtc, codigoCategoria, nombreCategoria, soportes, null, null, null, null);
    }
}
