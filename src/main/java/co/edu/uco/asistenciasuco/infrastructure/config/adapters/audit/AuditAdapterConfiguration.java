package co.edu.uco.asistenciasuco.infrastructure.config.adapters.audit;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.PurgaAuditoriaScheduler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.logging.LoggingAuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

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

    /**
     * Job diario de retencion de {@code dbo.AuditoriaEvento}: solo existe si hay persistencia durable de auditoria
     * (misma condicion que usa {@link LoggingAuditEventPublisher} para decidir si guarda en BD).
     */
    @Bean
    @ConditionalOnBean(AuditEventJpaRepository.class)
    public PurgaAuditoriaScheduler purgaAuditoriaScheduler(
            final AuditEventJpaRepository repository,
            @Value("${app.adapters.audit.purga.retencion:P4D}") final Duration retencion,
            @Value("${app.adapters.audit.purga.retardo-inicial:PT5M}") final Duration retardoInicial,
            @Value("${app.adapters.audit.purga.tick:PT24H}") final Duration tick
    ) {
        return new PurgaAuditoriaScheduler(repository::purgarAnterioresA, Clock.systemUTC(), retencion, retardoInicial, tick);
    }
}
