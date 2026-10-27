package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representacion HTTP de una solicitud de revision propia del estudiante (HU010/HU011): los mismos campos y la misma
 * codificacion que la bandeja docente (LB-004D) sin los datos del estudiante. Los instantes son cadenas UTC ISO-8601
 * con {@code Z}; {@code null} si el dato historico no tiene procedencia verificable.
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
        SoporteAdjuntoResponse soporteAdjunto
) {

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
