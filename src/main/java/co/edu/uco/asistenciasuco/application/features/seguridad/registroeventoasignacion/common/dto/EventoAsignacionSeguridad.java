package co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SEC-BASELINE-020: valor neutral de {@code AssignmentChangedEventV1}, el evento que el PDP (Security Baseline
 * 0.1.14) publica en RabbitMQ al asignar o revocar un rol o perfil. Compartido entre el puerto de entrada y el
 * puerto secundario de este feature para no acoplar uno al paquete del otro.
 */
public record EventoAsignacionSeguridad(
        UUID eventId,
        int schemaVersion,
        String eventType,
        OffsetDateTime occurredOn,
        String tenantId,
        UUID applicationId,
        String applicationName,
        String userId,
        String correlationId,
        String assignmentId,
        String roleId,
        String roleName,
        String profileAssignmentId,
        String profileId,
        String profileName
) {
}
