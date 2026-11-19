package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.audit;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.application.secondaryports.security.RegistroEventoAsignacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SEC-BASELINE-020: registra el evento de asignacion del PDP en {@code dbo.AuditoriaEvento}, reutilizando la
 * infraestructura de auditoria existente (LB-008 JPA-06B). Usa {@code eventId} como PK de la fila para que la
 * idempotencia quede garantizada por la restriccion unica de la tabla, no por un Set en memoria.
 *
 * <p>{@code httpMethod}, {@code path} y {@code httpStatus} son {@code NOT NULL} en la tabla real (diseñada para
 * auditoria HTTP, ver {@code AuditInterceptor}); esta es la primera fuente no HTTP, asi que se completan con un
 * valor de origen explicito en vez de null (comprobado contra la BD real en VALIDATION.md de este work item).</p>
 */
public final class RegistroEventoAsignacionAuditAdapter implements RegistroEventoAsignacionPort {

    static final String RESOURCE_TYPE = "SECURITY_ASSIGNMENT_EVENT";
    static final String HTTP_METHOD_ORIGEN_AMQP = "AMQP";
    static final String PATH_ORIGEN_AMQP = "rabbitmq:security.assignment.events";
    // CK_AuditoriaEvento_HttpStatus exige [100, 599] (comprobado contra la BD real): no admite un "0"/"N/A" como
    // sentinel de "sin HTTP". Se usa 200 (equivalente a "procesado correctamente"), igual que un httpStatus real
    // de exito; VALIDATION.md de este work item documenta la comprobacion.
    static final int HTTP_STATUS_PROCESADO = 200;

    private final AuditEventJpaRepository repository;

    public RegistroEventoAsignacionAuditAdapter(final AuditEventJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "AuditEventJpaRepository es obligatorio.");
    }

    @Override
    public boolean registrarSiNuevo(final EventoAsignacionSeguridad evento) {
        return repository.insertIfAbsent(toAuditEvent(evento));
    }

    private AuditEvent toAuditEvent(final EventoAsignacionSeguridad evento) {
        final String resourceId = evento.assignmentId() != null ? evento.assignmentId() : evento.profileAssignmentId();
        final OffsetDateTime occurredAt = evento.occurredOn() != null ? evento.occurredOn() : OffsetDateTime.now();
        return new AuditEvent(
                evento.eventId(),
                occurredAt,
                evento.userId(),
                AuditActorType.SYSTEM,
                evento.eventType(),
                RESOURCE_TYPE,
                resourceId,
                evento.correlationId(),
                null,
                null,
                HTTP_METHOD_ORIGEN_AMQP,
                PATH_ORIGEN_AMQP,
                HTTP_STATUS_PROCESADO,
                null,
                null,
                AuditOutcome.SUCCESS,
                metadata(evento));
    }

    private Map<String, String> metadata(final EventoAsignacionSeguridad evento) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("applicationId", String.valueOf(evento.applicationId()));
        metadata.put("tenantId", evento.tenantId());
        metadata.put("roleId", evento.roleId());
        metadata.put("roleName", evento.roleName());
        metadata.put("profileAssignmentId", evento.profileAssignmentId());
        metadata.put("profileId", evento.profileId());
        metadata.put("profileName", evento.profileName());
        return metadata;
    }
}
