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
        List<SoporteReclamoResponse> soportes
) {

    /** Causa registrada de la inasistencia y si el estudiante figura como asistente. */
    public record CausalInasistenciaResponse(String codigo, String nombre, Boolean asistio) {
    }
}
