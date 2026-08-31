package co.edu.uco.asistenciasuco.infrastructure.observability.audit;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
