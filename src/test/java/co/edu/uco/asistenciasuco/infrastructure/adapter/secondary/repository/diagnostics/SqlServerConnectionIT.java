package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.diagnostics;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.GrupoRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.TipoIdentificacionRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.UsuarioRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.diagnostics.DatabaseDiagnosticsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@SpringBootTest
class SqlServerConnectionIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseDiagnosticsService databaseDiagnosticsService;

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

        assertNotNull(dataSource);
        assertTrue(databaseDiagnosticsService.connectionIsValid());
        assertEquals(1, databaseDiagnosticsService.selectOne());
        assertEquals(expectedDatabaseName, databaseDiagnosticsService.currentDatabaseName());
        assertFalse(databaseDiagnosticsService.connectedUserName().isBlank());
        assertFalse(databaseDiagnosticsService.serverInfo().isEmpty());
        assertTrue(databaseDiagnosticsService.viewExists("dbo.uv_tipo_identificacion"));
        assertTrue(databaseDiagnosticsService.viewExists("dbo.uv_usuario"));
        assertNotNull(databaseDiagnosticsService.firstTipoIdentificacionRows());
        assertFalse(tipoIdentificacionRepositoryPort instanceof TipoIdentificacionRepositoryMockAdapter);
        assertFalse(usuarioRepositoryPort instanceof UsuarioRepositoryMockAdapter);
        assertNotNull(docenteRepositoryPort);
        assertFalse(grupoRepositoryPort instanceof GrupoRepositoryMockAdapter);
        assertEquals("servlet", environment.getProperty("spring.main.web-application-type"));
        assertEquals("true", environment.getProperty("spring.threads.virtual.enabled"));
    }
}
