package co.edu.uco.asistenciasuco.infrastructure.audit.contract;

import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
