package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.DocenteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.EstudianteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.GrupoRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.TipoIdentificacionRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.UsuarioRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SpringPasswordEncoderAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.password.PasswordEncoderAdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionOperations;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * Verifica que el Composition Root de SQL Server (no las Feature Configs) es quien
 * construye los adapters tecnológicos, y que el resultado sigue siendo el mismo adapter
 * concreto que antes del Prompt 1.
 */
class FeaturesBeansConfigTest {

    private final SqlServerCoreRepositoryAdapterConfiguration config = new SqlServerCoreRepositoryAdapterConfiguration();

    @Test
    void tipoIdentificacionRepositoryPort_usa_sqlserver() {
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final TipoIdentificacionRepositoryPort repositoryPort =
                config.tipoIdentificacionRepositoryPort(jdbcTemplate);

        assertInstanceOf(TipoIdentificacionRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void usuarioRepositoryPort_usa_sqlserver() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);

        final UsuarioRepositoryPort repositoryPort = config.usuarioRepositoryPort(procedureExecutor, jdbcOperations);

        assertInstanceOf(UsuarioRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void docenteRepositoryPort_usa_sqlserver() {
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final DocenteRepositoryPort repositoryPort = config.docenteRepositoryPort(jdbcTemplate);

        assertInstanceOf(DocenteRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void grupoRepositoryPort_usa_sqlserver() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final TransactionOperations transactionOperations = mock(TransactionOperations.class);

        final GrupoRepositoryPort repositoryPort = config.grupoRepositoryPort(
                procedureExecutor,
                jdbcOperations,
                transactionOperations
        );

        assertInstanceOf(GrupoRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void estudianteRepositoryPort_usa_sqlserver() {
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final EstudianteRepositoryPort repositoryPort = config.estudianteRepositoryPort(jdbcTemplate);

        assertInstanceOf(EstudianteRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void passwordEncoderPort_usa_adapter_spring_crypto_sin_security_http() {
        final PasswordEncoderAdapterConfiguration passwordConfig = new PasswordEncoderAdapterConfiguration();

        final PasswordEncoderPort passwordEncoderPort = passwordConfig.passwordEncoderPort();

        assertInstanceOf(SpringPasswordEncoderAdapter.class, passwordEncoderPort);
    }
}
