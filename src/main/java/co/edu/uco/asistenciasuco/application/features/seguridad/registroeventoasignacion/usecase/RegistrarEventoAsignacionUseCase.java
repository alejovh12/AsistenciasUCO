package co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort;

/**
 * SEC-BASELINE-020: caso de uso que audita los eventos de asignacion de rol/perfil del PDP, una sola vez por
 * {@code eventId}.
 */
public interface RegistrarEventoAsignacionUseCase extends RegistrarEventoAsignacionInputPort {
}
