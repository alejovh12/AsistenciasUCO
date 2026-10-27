package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response;

import java.util.UUID;

/** Representacion HTTP de una solicitud de ingreso propia (HU053/HU055); fechas UTC ISO-8601 con {@code Z}. */
public record SolicitudIngresoEstudianteResponse(
        UUID id,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID asignaturaId,
        String asignaturaNombre,
        String docenteNombre,
        String estado,
        String fechaSolicitudUtc,
        String fechaResolucionUtc,
        String motivoRechazo
) {
}
