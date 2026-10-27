package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de ingreso propia con el estado ya en forma legible. */
public record SolicitudIngresoEstudianteDomain(
        UUID id,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID asignaturaId,
        String asignaturaNombre,
        String docenteNombre,
        String estado,
        Instant fechaSolicitudUtc,
        Instant fechaResolucionUtc,
        String motivoRechazo
) {
}
