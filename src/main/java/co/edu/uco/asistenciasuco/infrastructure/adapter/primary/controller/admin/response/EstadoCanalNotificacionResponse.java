package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.time.Instant;

/**
 * Estado del canal de correo para el administrador (CULM-001 D.9-a, F-ADM-029). Solo si el canal esta activo, el instante UTC de
 * la ultima entrega y el codigo del ultimo error vigente: nunca el host, el puerto ni el usuario de la cuenta tecnica.
 */
public record EstadoCanalNotificacionResponse(
        boolean canalActivo,
        Instant ultimoEnvioUtc,
        String ultimoError
) {
}
