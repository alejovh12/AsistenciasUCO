package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.CategoriaReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.SoporteReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de una solicitud de revision para el docente titular (HU036). {@code fechaSesionUtc} es un instante UTC
 * (nulo si la hora historica de la sesion no tiene procedencia verificable). {@code causalInasistencia} es nula si
 * la asistencia no tiene detalle registrado; {@code soporteAdjunto} es nulo si no hay archivo adjunto.
 * CULM-001 D.2: {@code categoria} (nula en reclamos anteriores a la categoria persistida) y {@code soportes} (todos, en orden de
 * adjuncion). {@code soporteAdjunto} (el primero) queda deprecado una version.
 */
public record DetalleReclamoDocenteDTO(
        UUID id,
        String nombre,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String estadoSolicitud,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID estudianteId,
        String estudianteNombre,
        UUID sesionId,
        Integer sesionNumero,
        Instant fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        CausalInasistenciaDTO causalInasistencia,
        ReclamoDocenteDTO.SoporteAdjuntoDTO soporteAdjunto,
        CategoriaReclamoDTO categoria,
        List<SoporteReclamoDTO> soportes,
        UUID idToma,
        Integer numeroToma,
        Instant fechaPuntoControlUtc,
        String resultadoAprobacion,
        String estadoToma,
        Integer minutosSesion,
        Integer minutosAsistidos
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public DetalleReclamoDocenteDTO(
            final UUID id,
            final String nombre,
            final LocalDate fechaSolicitud,
            final String codigoEstado,
            final String estadoSolicitud,
            final String nombreEstado,
            final String justificacionSolicitud,
            final String justificacionRespuesta,
            final UUID estudianteId,
            final String estudianteNombre,
            final UUID sesionId,
            final Integer sesionNumero,
            final Instant fechaSesionUtc,
            final String procedenciaTemporalSesion,
            final UUID grupoId,
            final Integer grupoCodigo,
            final String grupoNombre,
            final UUID materiaId,
            final String materiaCodigo,
            final String materiaNombre,
            final CausalInasistenciaDTO causalInasistencia,
            final ReclamoDocenteDTO.SoporteAdjuntoDTO soporteAdjunto,
            final CategoriaReclamoDTO categoria,
            final List<SoporteReclamoDTO> soportes
    ) {
        this(id, nombre, fechaSolicitud, codigoEstado, estadoSolicitud, nombreEstado, justificacionSolicitud, justificacionRespuesta, estudianteId, estudianteNombre, sesionId, sesionNumero, fechaSesionUtc, procedenciaTemporalSesion, grupoId, grupoCodigo, grupoNombre, materiaId, materiaCodigo, materiaNombre, causalInasistencia, soporteAdjunto, categoria, soportes, null, null, null, null, null, null, null);
    }

    /** Causa registrada de la inasistencia ({@code RazonCausa}) y si el estudiante figura como asistente. */
    public record CausalInasistenciaDTO(String codigo, String nombre, Boolean asistio) {
    }
}
