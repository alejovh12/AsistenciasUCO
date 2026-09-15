package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.diagnostics;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble.GrupoRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble.TipoIdentificacionRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble.UsuarioRepositoryMockAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SqlServerConnectionIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private DocenteRepositoryPort docenteRepositoryPort;

    @Autowired
    private GrupoRepositoryPort grupoRepositoryPort;

    @Test
    void sqlServer_connection_is_real_and_database_contract_is_available() {
        final String expectedDatabaseName = environment.getRequiredProperty("APP_DATABASE_EXPECTED_NAME");
        final SqlServerTestDiagnostics databaseDiagnostics = new SqlServerTestDiagnostics(dataSource, jdbcTemplate);

        assertNotNull(dataSource);
        assertTrue(databaseDiagnostics.connectionIsValid());
        assertEquals(1, databaseDiagnostics.selectOne());
        assertEquals(expectedDatabaseName, databaseDiagnostics.currentDatabaseName());
        assertFalse(databaseDiagnostics.connectedUserName().isBlank());
        assertFalse(databaseDiagnostics.serverInfo().isEmpty());
        assertTrue(databaseDiagnostics.viewExists("dbo.uv_tipo_identificacion"));
        assertTrue(databaseDiagnostics.viewExists("dbo.uv_usuario"));
        assertNotNull(databaseDiagnostics.firstTipoIdentificacionRows());
        assertFalse(tipoIdentificacionRepositoryPort instanceof TipoIdentificacionRepositoryMockAdapter);
        assertFalse(usuarioRepositoryPort instanceof UsuarioRepositoryMockAdapter);
        assertNotNull(docenteRepositoryPort);
        assertFalse(grupoRepositoryPort instanceof GrupoRepositoryMockAdapter);
        assertEquals("servlet", environment.getProperty("spring.main.web-application-type"));
        assertEquals("true", environment.getProperty("spring.threads.virtual.enabled"));
    }
}
