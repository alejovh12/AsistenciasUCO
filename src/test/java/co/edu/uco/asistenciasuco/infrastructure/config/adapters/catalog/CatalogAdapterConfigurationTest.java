package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver.SqlServerMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver.SqlServerParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigMessageCatalogAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCatalogAdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogAdapterConfigurationTest {

    @Configuration
    static class MockJdbcConfiguration {
        @Bean
        public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
            return Mockito.mock(NamedParameterJdbcTemplate.class);
        }
    }

    private final ApplicationContextRunner sqlContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MockJdbcConfiguration.class,
                    SqlServerCatalogAdapterConfiguration.class
            )
            .withPropertyValues(
                    "app.adapters.parameter-catalog.provider=sqlserver",
                    "app.adapters.message-catalog.provider=sqlserver"
            );

    private final ApplicationContextRunner missingAppConfigRunner = new ApplicationContextRunner()
            .withUserConfiguration(AzureAppConfigAdapterConfiguration.class)
            .withPropertyValues(
                    "app.adapters.parameter-catalog.provider=azure_appconfig",
                    "app.providers.azure-appconfig.endpoint="
            );

    @Test
    void registra_adaptadores_sql_cuando_provider_es_sqlserver() {
        sqlContextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(ParameterCatalogPort.class).size());
            assertInstanceOf(SqlServerParameterCatalogAdapter.class, context.getBean(ParameterCatalogPort.class));

            assertEquals(1, context.getBeansOfType(MessageCatalogPort.class).size());
            assertInstanceOf(SqlServerMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class));
        });
    }

    @Test
    void falla_fail_fast_si_endpoint_azure_appconfig_esta_vacio() {
        missingAppConfigRunner.run(context -> {
            assertTrue(context.getStartupFailure() != null);
        });
    }

    @Test
    void registra_azure_message_catalog_adapter_cuando_provider_es_azure() {
        new ApplicationContextRunner()
                .withUserConfiguration(AzureAppConfigMessageCatalogAdapterConfiguration.class)
                .withPropertyValues(
                        "app.adapters.message-catalog.provider=azure",
                        "app.providers.azure-appconfig.endpoint=https://appcs-asist-dev-29183.azconfig.io"
                )
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(MessageCatalogPort.class).size());
                    assertInstanceOf(AzureAppConfigMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class));
                });
    }
}
