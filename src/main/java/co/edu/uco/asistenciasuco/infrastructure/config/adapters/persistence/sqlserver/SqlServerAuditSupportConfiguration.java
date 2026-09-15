package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.audit.adapter.sqlserver.AuditEventJdbcRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerAuditSupportConfiguration {

    @Bean
    public AuditEventJdbcRepository auditEventJdbcRepository(
            final ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        return new AuditEventJdbcRepository(jdbcTemplateProvider);
    }
}
