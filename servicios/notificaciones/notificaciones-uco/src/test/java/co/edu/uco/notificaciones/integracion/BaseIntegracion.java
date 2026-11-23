package co.edu.uco.notificaciones.integracion;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL Server real (Testcontainers) y un SMTP embebido (GreenMail) compartidos por las pruebas de integracion. Sin Docker se omiten.
 * La imagen es la misma que usa el laboratorio (SQL Server 2022 CU25).
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class BaseIntegracion {

    private static final String IMAGEN = "mcr.microsoft.com/mssql/server@sha256:fc010e5c955453eabc4a24d2f304103e26176fe89796b564f039c45e77a8ebc1";

    static MSSQLServerContainer sql;
    static GreenMail smtp;
    static int puertoSmtp;

    @BeforeAll
    static void arrancarInfraestructura() throws IOException, SQLException {
        if (sql != null) {
            return;
        }
        sql = new MSSQLServerContainer(DockerImageName.parse(IMAGEN).asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
                .acceptLicense()
                .withEnv("MSSQL_MEMORY_LIMIT_MB", "1024");
        sql.start();
        try (Connection c = DriverManager.getConnection(sql.getJdbcUrl(), sql.getUsername(), sql.getPassword());
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE notificacionesdb");
        }
        try (ServerSocket libre = new ServerSocket(0)) {
            puertoSmtp = libre.getLocalPort();
        }
        smtp = new GreenMail(new ServerSetup(puertoSmtp, "127.0.0.1", "smtp"));
        smtp.start();
    }

    @AfterAll
    static void noDetenerEntreClases() {
        // El contenedor y el SMTP viven toda la JVM de pruebas (Ryuk limpia el contenedor al terminar).
    }

    @DynamicPropertySource
    static void propiedades(final DynamicPropertyRegistry registro) throws IOException, SQLException {
        arrancarInfraestructura();
        registro.add("NOTIFICACIONES_DB_URL", () -> "jdbc:sqlserver://" + sql.getHost() + ":" + sql.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT)
                + ";databaseName=notificacionesdb;encrypt=true;trustServerCertificate=true");
        registro.add("spring.datasource.url", () -> "jdbc:sqlserver://" + sql.getHost() + ":" + sql.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT)
                + ";databaseName=notificacionesdb;encrypt=true;trustServerCertificate=true");
        registro.add("spring.datasource.username", sql::getUsername);
        registro.add("spring.datasource.password", sql::getPassword);
        registro.add("notificaciones.api-key", () -> "clave-integracion");
        registro.add("notificaciones.canal", () -> "smtp");
        registro.add("notificaciones.smtp.host", () -> "127.0.0.1");
        registro.add("notificaciones.smtp.port", () -> String.valueOf(puertoSmtp));
        registro.add("notificaciones.smtp.starttls", () -> "false");
        registro.add("notificaciones.smtp.from-address", () -> "noreply@uco.edu.co");
        registro.add("notificaciones.smtp.platform-url", () -> "http://localhost:4200");
        registro.add("notificaciones.smtp.connect-timeout", () -> "1s");
        registro.add("notificaciones.smtp.read-timeout", () -> "2s");
        // El ciclo del despachador lo dispara cada prueba a mano: nada corre en segundo plano.
        registro.add("notificaciones.dispatcher.enabled", () -> "false");
        registro.add("notificaciones.dispatcher.instance-id", () -> "it-1");
    }
}
