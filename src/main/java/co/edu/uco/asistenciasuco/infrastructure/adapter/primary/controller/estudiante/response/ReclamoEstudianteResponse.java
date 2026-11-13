package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CategoriaReclamoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.SoporteReclamoResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representacion HTTP de una solicitud de revision propia del estudiante (HU010/HU011): los mismos campos y la misma
 * codificacion que la bandeja docente (LB-004D) sin los datos del estudiante. Los instantes son cadenas UTC ISO-8601
 * con {@code Z}; {@code null} si el dato historico no tiene procedencia verificable.
 * CULM-001 D.2: {@code categoria} ({@code null} en reclamos anteriores a la categoria persistida) y {@code soportes} (todos, en orden
 * de adjuncion); {@code soporteAdjunto} (el primero) queda deprecado una version.
 */
public record ReclamoEstudianteResponse(
        UUID id,
        String nombre,
        LocalDate fechaSolicitud,
        String estadoSolicitud,
        String justificacionSolicitud,
        String justificacionRespuesta,
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
        SoporteAdjuntoResponse soporteAdjunto,
        CategoriaReclamoResponse categoria,
        List<SoporteReclamoResponse> soportes,
        UUID idToma,
        Integer numeroToma,
        String fechaPuntoControlUtc,
        String resultadoAprobacion
) {

    /** Compatibilidad: sin los datos de la toma (PC, puntos de control). */
    public ReclamoEstudianteResponse(
            final UUID id,
            final String nombre,
            final LocalDate fechaSolicitud,
            final String estadoSolicitud,
            final String justificacionSolicitud,
            final String justificacionRespuesta,
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
            final SoporteAdjuntoResponse soporteAdjunto,
            final CategoriaReclamoResponse categoria,
            final List<SoporteReclamoResponse> soportes
    ) {
        this(id, nombre, fechaSolicitud, estadoSolicitud, justificacionSolicitud, justificacionRespuesta, sesionId, sesionNumero, fechaSesionUtc, procedenciaTemporalSesion, grupoId, grupoCodigo, grupoNombre, materiaId, materiaCodigo, materiaNombre, soporteAdjunto, categoria, soportes, null, null, null, null);
    }

    /** Soporte vinculado: se descarga por {@code GET /api/v1/archivos/{fileId}} autenticado, nunca por el storage. */
    public record SoporteAdjuntoResponse(
            UUID fileId,
            String nombre,
            String tipo,
            long tamanioBytes,
            String fechaSubidaUtc,
            String url
    ) {
    }
}
