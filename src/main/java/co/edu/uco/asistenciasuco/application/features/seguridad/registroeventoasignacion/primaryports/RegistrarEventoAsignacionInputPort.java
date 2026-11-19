package co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;

/**
 * SEC-BASELINE-020: puerto de entrada que registra, de forma idempotente, un evento de asignacion de rol/perfil
 * publicado por el PDP (Security Baseline 0.1.14) en RabbitMQ.
 *
 * <p>Lo invoca el adaptador primario de mensajeria ({@code AssignmentEventsRabbitListener}). El unico efecto local
 * es auditoria para trazabilidad: este puerto no cambia ninguna decision de autorizacion.</p>
 */
public interface RegistrarEventoAsignacionInputPort {

    /**
     * @throws EventoAsignacionInvalidoException si el evento no trae {@code eventId}, su {@code schemaVersion} no es
     *                                            la soportada, o su {@code applicationId} no es el de esta aplicacion.
     */
    ResultadoRegistro procesar(EventoAsignacionSeguridad evento);

    enum ResultadoRegistro {
        /** Primera vez que se ve este {@code eventId}: se registro. */
        PROCESADO,
        /** Entrega repetida (al-menos-una-vez) del mismo {@code eventId}: no se repite el efecto. */
        DUPLICADO
    }

    /** Contrato invalido (schemaVersion no soportada o applicationId ajeno): no se corrige reintentando. */
    final class EventoAsignacionInvalidoException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public EventoAsignacionInvalidoException(final String message) {
            super(message);
        }
    }
}
