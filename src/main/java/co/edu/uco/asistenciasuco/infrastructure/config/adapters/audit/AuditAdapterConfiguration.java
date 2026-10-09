package co.edu.uco.asistenciasuco.infrastructure.config.adapters.audit;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.logging.LoggingAuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el publicador de auditoría.
 *
 * <p>Esta configuración solo crea el {@link AuditEventPublisher}. El provider {@code LOGGING} emite siempre log
 * estructurado y usa persistencia durable cuando {@code AuditEventJpaRepository} está disponible: ese repositorio
 * se descubre como {@code @Repository} normal y NO lo registra manualmente este Composition Root.</p>
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
            final ObjectProvider<AuditEventJpaRepository> repositoryProvider
    ) {
        return new LoggingAuditEventPublisher(repositoryProvider);
    }
}
