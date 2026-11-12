package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.domain.SoporteReclamoDomain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalle de la solicitud de revision visible para el docente titular (instantes UTC; nulos sin procedencia verificable). */
public record DetalleReclamoDocenteDomain(
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
        List<SoporteReclamoDomain> soportes,
        UUID idToma,
        Integer numeroToma,
        Instant fechaPuntoControlUtc,
        String codigoResultadoAprobacion,
        String codigoRazonCausaToma,
        Integer minutosSesion,
        Integer minutosAsistidos
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public DetalleReclamoDocenteDomain(
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
            final List<SoporteReclamoDomain> soportes
    ) {
        this(idSolicitud, nombreSolicitud, fechaSolicitud, codigoEstado, nombreEstado, justificacionSolicitud, justificacionRespuesta, idEstudiante, nombreEstudiante, idSesion, numeroSesion, fechaHoraInicioSesionUtc, procedenciaTemporalSesion, idGrupo, codigoGrupo, nombreGrupo, idAsignatura, codigoAsignatura, nombreAsignatura, asistio, codigoRazonCausa, nombreRazonCausa, idArchivoSoporte, nombreSoporte, tipoContenidoSoporte, tamanioSoporteBytes, fechaRegistroSoporteUtc, codigoCategoria, nombreCategoria, soportes, null, null, null, null, null, null, null);
    }
}
