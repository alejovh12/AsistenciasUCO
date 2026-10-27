package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response;

import java.util.UUID;

/**
 * Representacion HTTP de una solicitud de ingreso en la bandeja del docente (HU053-HU055). Las fechas son instantes
 * UTC ISO-8601 con {@code Z}; {@code posicionEspera} solo aplica a EN_ESPERA.
 */
public record SolicitudIngresoDocenteResponse(
        UUID id,
        UUID estudianteId,
        String estudianteNombre,
        Integer numeroIdentificacion,
        String estado,
        String fechaSolicitudUtc,
        String fechaResolucionUtc,
        String motivoRechazo,
        Integer posicionEspera,
        int capacidadGrupo,
        int cuposDisponibles
) {
}
