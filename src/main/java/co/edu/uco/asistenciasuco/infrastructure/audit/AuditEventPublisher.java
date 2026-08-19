package co.edu.uco.asistenciasuco.infrastructure.audit;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
