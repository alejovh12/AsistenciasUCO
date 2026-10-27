package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de ingreso propia con el grupo, la asignatura, el docente, el estado legible y el motivo de rechazo. */
public record SolicitudIngresoEstudianteDTO(
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
