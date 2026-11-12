package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CategoriaReclamoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.SoporteReclamoResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representacion HTTP del detalle de una solicitud de revision para el docente titular (HU036). Los instantes son
 * cadenas UTC ISO-8601 con {@code Z} (contrato UTC v2); {@code fechaSesionUtc} es {@code null} si el dato historico
 * no tiene procedencia verificable. {@code causalInasistencia} es {@code null} sin detalle de asistencia y
 * {@code soporteAdjunto} es {@code null} sin archivo adjunto (misma forma que en la bandeja).
 * CULM-001 D.2: {@code categoria} ({@code null} en reclamos anteriores a la categoria persistida) y {@code soportes} (todos, en orden
 * de adjuncion); {@code soporteAdjunto} (el primero) queda deprecado una version.
 */
public record DetalleReclamoDocenteResponse(
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
        String fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        CausalInasistenciaResponse causalInasistencia,
        ReclamoDocenteResponse.SoporteAdjuntoResponse soporteAdjunto,
        CategoriaReclamoResponse categoria,
        List<SoporteReclamoResponse> soportes,
        UUID idToma,
        Integer numeroToma,
        String fechaPuntoControlUtc,
        String resultadoAprobacion,
        String estadoToma,
        Integer minutosSesion,
        Integer minutosAsistidos
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public DetalleReclamoDocenteResponse(
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
            final String fechaSesionUtc,
            final String procedenciaTemporalSesion,
            final UUID grupoId,
            final Integer grupoCodigo,
            final String grupoNombre,
            final UUID materiaId,
            final String materiaCodigo,
            final String materiaNombre,
            final CausalInasistenciaResponse causalInasistencia,
            final ReclamoDocenteResponse.SoporteAdjuntoResponse soporteAdjunto,
            final CategoriaReclamoResponse categoria,
            final List<SoporteReclamoResponse> soportes
    ) {
        this(id, nombre, fechaSolicitud, codigoEstado, estadoSolicitud, nombreEstado, justificacionSolicitud, justificacionRespuesta, estudianteId, estudianteNombre, sesionId, sesionNumero, fechaSesionUtc, procedenciaTemporalSesion, grupoId, grupoCodigo, grupoNombre, materiaId, materiaCodigo, materiaNombre, causalInasistencia, soporteAdjunto, categoria, soportes, null, null, null, null, null, null, null);
    }

    /** Causa registrada de la inasistencia y si el estudiante figura como asistente. */
    public record CausalInasistenciaResponse(String codigo, String nombre, Boolean asistio) {
    }
}
