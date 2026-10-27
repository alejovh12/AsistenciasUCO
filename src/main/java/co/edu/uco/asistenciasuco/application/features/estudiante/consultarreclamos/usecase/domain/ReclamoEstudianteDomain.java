package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain;

import java.time.Instant;
import java.time.LocalDate;
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
        Instant fechaRegistroSoporteUtc
) {
}
