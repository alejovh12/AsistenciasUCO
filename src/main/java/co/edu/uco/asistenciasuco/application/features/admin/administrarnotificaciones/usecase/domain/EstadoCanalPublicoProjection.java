package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;

/**
 * Estado del canal de correo sin datos de la cuenta tecnica (CULM-001 D.9-a, F-ADM-029): si el canal esta activo, el instante UTC
 * de la ultima entrega y el codigo del ultimo error vigente (el de una falla posterior a la ultima entrega, o {@code null}).
 */
public record EstadoCanalPublicoProjection(boolean canalActivo, Instant ultimoEnvioUtc, String ultimoError) {
}
