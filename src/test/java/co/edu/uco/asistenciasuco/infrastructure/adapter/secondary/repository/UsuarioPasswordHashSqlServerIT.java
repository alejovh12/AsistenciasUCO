package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
class UsuarioPasswordHashSqlServerIT {

    @Autowired
    private CrearUsuarioInputPort crearUsuarioInputPort;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_usuario_persiste_hash_verificable_y_no_password_plano() {
        final Optional<UUID> tipoIdentificacion = firstUuid("""
                SELECT TOP 1 id
                FROM dbo.uv_tipo_identificacion
                ORDER BY tipoIdentificacion
                """);
        assumeTrue(tipoIdentificacion.isPresent(), "No hay tipos de identificacion para ejecutar la IT.");

        final TestIdentity identity = uniqueIdentity();
        final String rawPassword = "PasswordValida123!";
        CorrelationIdContext.set(UUID.randomUUID());

        transactionTemplate.executeWithoutResult(status -> {
            assertDoesNotThrow(() -> crearUsuarioInputPort.execute(new CrearUsuarioDTO(
                    tipoIdentificacion.get(),
                    identity.numeroIdentificacion(),
                    "PEREZ",
                    "GOMEZ",
                    "ANA",
                    "MARIA",
                    identity.correo(),
                    rawPassword
            )));

            final String persistedPassword = jdbcTemplate.queryForObject(
                    """
                            SELECT password
                            FROM dbo.Usuario
                            WHERE correo = ?
                            """,
                    String.class,
                    identity.correo()
            );

            assertNotEquals(rawPassword, persistedPassword);
            assertTrue(passwordEncoderPort.matches(rawPassword, persistedPassword));
            status.setRollbackOnly();
        });

        assertEquals(0, usuarioCount(identity));
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }

    private int usuarioCount(final TestIdentity identity) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM dbo.Usuario
                        WHERE correo = ?
                           OR numeroIdentificacion = ?
                        """,
                Integer.class,
                identity.correo(),
                identity.numeroIdentificacion()
        );
    }

    private TestIdentity uniqueIdentity() {
        final String suffix = UUID.randomUUID().toString().replace("-", "");
        final int numeroIdentificacion = 700000000 + ThreadLocalRandom.current().nextInt(100000000);
        return new TestIdentity(
                numeroIdentificacion,
                "it.hash." + suffix.substring(0, 20) + "@uco.edu.co"
        );
    }

    private record TestIdentity(Integer numeroIdentificacion, String correo) {
    }
}
