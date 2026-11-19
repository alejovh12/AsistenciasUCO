package co.edu.uco.asistenciasuco.application.secondaryports.security;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;

/**
 * SEC-BASELINE-020: puerto secundario que persiste de forma durable e idempotente un evento de asignacion ya
 * validado por el caso de uso.
 */
public interface RegistroEventoAsignacionPort {

    /**
     * Registra el evento si su {@code eventId} no se habia procesado antes.
     *
     * @return {@code true} si quedo registrado (primera vez); {@code false} si ya existia (duplicado).
     */
    boolean registrarSiNuevo(EventoAsignacionSeguridad evento);
}
