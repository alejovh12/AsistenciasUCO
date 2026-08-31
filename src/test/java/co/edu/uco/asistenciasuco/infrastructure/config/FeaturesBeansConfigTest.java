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
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SpringPasswordEncoderAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.features.DocenteBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.EstudianteBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.GrupoBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.SecurityBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.TipoIdentificacionBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.UsuarioBeansConfig;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class FeaturesBeansConfigTest {

    @Test
    void tipoIdentificacionRepositoryPort_usa_sqlserver_sin_selector_de_modo() {
        final TipoIdentificacionBeansConfig config = new TipoIdentificacionBeansConfig();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final TipoIdentificacionRepositoryPort repositoryPort =
                config.tipoIdentificacionRepositoryPort(jdbcTemplate);

        assertInstanceOf(TipoIdentificacionRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void usuarioRepositoryPort_usa_sqlserver_sin_selector_de_modo() {
        final UsuarioBeansConfig config = new UsuarioBeansConfig();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final UsuarioRepositoryPort repositoryPort = config.usuarioRepositoryPort(jdbcTemplate);

        assertInstanceOf(UsuarioRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void docenteRepositoryPort_usa_sqlserver_sin_selector_de_modo() {
        final DocenteBeansConfig config = new DocenteBeansConfig();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final DocenteRepositoryPort repositoryPort = config.docenteRepositoryPort(jdbcTemplate);

        assertInstanceOf(DocenteRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void grupoRepositoryPort_usa_sqlserver_con_transaction_template() {
        final GrupoBeansConfig config = new GrupoBeansConfig();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

        final GrupoRepositoryPort repositoryPort = config.grupoRepositoryPort(jdbcTemplate, transactionTemplate);

        assertInstanceOf(GrupoRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void estudianteRepositoryPort_usa_sqlserver_sin_selector_de_modo() {
        final EstudianteBeansConfig config = new EstudianteBeansConfig();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        final EstudianteRepositoryPort repositoryPort = config.estudianteRepositoryPort(jdbcTemplate);

        assertInstanceOf(EstudianteRepositorySqlServerAdapter.class, repositoryPort);
    }

    @Test
    void passwordEncoderPort_usa_adapter_spring_crypto_sin_security_http() {
        final SecurityBeansConfig config = new SecurityBeansConfig();

        final PasswordEncoderPort passwordEncoderPort = config.passwordEncoderPort();

        assertInstanceOf(SpringPasswordEncoderAdapter.class, passwordEncoderPort);
    }
}
