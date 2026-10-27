package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitud de ingreso en la bandeja del docente. {@code posicionEspera} es el orden de llegada dentro de la lista de
 * espera (informativo: el docente puede aceptar a cualquiera); {@code cuposDisponibles} = capacidad - activos.
 */
public record SolicitudIngresoDocenteDTO(
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
