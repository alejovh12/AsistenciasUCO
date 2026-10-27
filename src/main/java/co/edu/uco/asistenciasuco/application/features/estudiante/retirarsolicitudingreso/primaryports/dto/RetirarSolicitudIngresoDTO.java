package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto;

import java.util.UUID;

/** Entrada: el estudiante propietario retira su solicitud abierta (PENDIENTE o EN_ESPERA). */
public record RetirarSolicitudIngresoDTO(UUID actorUsuarioId, UUID solicitudId) {
}
