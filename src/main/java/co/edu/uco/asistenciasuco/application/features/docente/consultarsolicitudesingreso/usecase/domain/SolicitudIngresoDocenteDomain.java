package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de la bandeja del docente con el estado ya en forma legible. */
public record SolicitudIngresoDocenteDomain(
        UUID id,
        UUID estudianteId,
        String estudianteNombre,
        Integer numeroIdentificacion,
        String estado,
        Instant fechaSolicitudUtc,
        Instant fechaResolucionUtc,
        String motivoRechazo,
        Integer posicionEspera,
        int capacidadGrupo,
        int cuposDisponibles
) {
}
