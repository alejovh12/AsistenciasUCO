package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerProcedureSupportConfiguration {

    @Bean
    public CanonicalStoredProcedureExecutor canonicalStoredProcedureExecutor(
            final NamedParameterJdbcOperations operations
    ) {
        return new CanonicalStoredProcedureExecutor(operations);
    }
}
