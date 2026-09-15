package co.edu.uco.asistenciasuco.infrastructure.config.adapters.audit;

import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.sqlserver.AuditEventJdbcRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.logging.LoggingAuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el publicador de auditoría.
 *
 * <p>El provider {@code LOGGING} emite siempre log estructurado y usa persistencia durable
 * cuando el Composition Root de SQL Server registra el repositorio de auditoría.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.audit",
        name = "provider",
        havingValue = "logging",
        matchIfMissing = true
)
public class AuditAdapterConfiguration {

    @Bean
    public AuditEventPublisher auditEventPublisher(
            final ObjectProvider<AuditEventJdbcRepository> repositoryProvider
    ) {
        return new LoggingAuditEventPublisher(repositoryProvider);
    }
}
