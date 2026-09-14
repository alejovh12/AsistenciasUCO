package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SqlServerPersistenceCompositionRootTest {

    @Test
    void persistence_provider_sqlserver_registra_ports_sqlserver_representativos() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        final TransactionOperations transactionOperations = mock(TransactionOperations.class);

        new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.provider=sqlserver")
                .withBean(NamedParameterJdbcOperations.class, () -> jdbcOperations)
                .withBean(JdbcTemplate.class, () -> jdbcTemplate)
                .withBean(TransactionOperations.class, () -> transactionOperations)
                .withUserConfiguration(
                        SqlServerProcedureSupportConfiguration.class,
                        SqlServerCoreRepositoryAdapterConfiguration.class,
                        SqlServerSecurityScopeAdapterConfiguration.class
                )
                .run(context -> {
                    assertNotNull(context.getBean(GrupoRepositoryPort.class));
                    assertNotNull(context.getBean(SesionRepositoryPort.class));
                    assertNotNull(context.getBean(AsistenciaRepositoryPort.class));
                    assertNotNull(context.getBean(InstitutionalScopePort.class));
                });
    }

    @Test
    void persistence_provider_explicitamente_distinto_no_registra_sqlserver_como_fallback() {
        new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.provider=something-else")
                .withUserConfiguration(
                        SqlServerProcedureSupportConfiguration.class,
                        SqlServerCoreRepositoryAdapterConfiguration.class,
                        SqlServerSecurityScopeAdapterConfiguration.class
                )
                .run(context -> {
                    assertFalse(context.containsBean("grupoRepositoryPort"));
                    assertFalse(context.containsBean("sesionRepositoryPort"));
                    assertFalse(context.containsBean("asistenciaRepositoryPort"));
                    assertFalse(context.containsBean("institutionalScopePort"));
                });
    }
}
