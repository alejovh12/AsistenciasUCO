package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver.SqlServerMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver.SqlServerParameterCatalogAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Composition Root de persistencia SQL Server para los catalogos institucionales.
 */
@Configuration(proxyBeanMethods = false)
public class SqlServerCatalogAdapterConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.adapters.parameter-catalog",
            name = "provider",
            havingValue = "sqlserver"
    )
    public ParameterCatalogPort sqlServerParameterCatalogPort(final NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlServerParameterCatalogAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.adapters.message-catalog",
            name = "provider",
            havingValue = "sqlserver"
    )
    public MessageCatalogPort sqlServerMessageCatalogPort(final NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlServerMessageCatalogAdapter(jdbcTemplate);
    }
}
