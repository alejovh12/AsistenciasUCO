package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AuthenticationRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioAutenticacionData;
import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioPerfilData;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
class AuthenticationRepositorySqlServerIT {

    @Autowired
    private CrearUsuarioInputPort crearUsuarioInputPort;

    @Autowired
    private AuthenticationRepositoryPort authenticationRepositoryPort;

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
    void consultar_credencial_por_correo_recupera_hash_desde_vista_autenticacion() {
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

            final Optional<UsuarioAutenticacionData> credencial =
                    authenticationRepositoryPort.consultarPorCorreo(identity.correo());

            assertTrue(credencial.isPresent());
            assertEquals(identity.correo(), credencial.get().getCorreo());
            assertNotEquals(rawPassword, credencial.get().getPasswordHash());
            assertTrue(passwordEncoderPort.matches(rawPassword, credencial.get().getPasswordHash()));
            assertTrue(authenticationRepositoryPort.consultarPerfilesPorUsuario(
                    credencial.get().getIdUsuario()
            ).isEmpty());
            status.setRollbackOnly();
        });

        assertEquals(0, usuarioCount(identity));
    }

    @Test
    void consultar_credencial_por_correo_inexistente_retorna_optional_vacio() {
        final String correoInexistente = "it.auth.noexiste."
                + UUID.randomUUID().toString().replace("-", "")
                + "@uco.edu.co";

        assertTrue(authenticationRepositoryPort.consultarPorCorreo(correoInexistente).isEmpty());
    }

    @Test
    void consultar_perfiles_por_usuario_coincide_con_uv_usuario_perfil() {
        final Optional<UUID> usuarioConPerfil = firstUuid("""
                SELECT TOP 1 idUsuario AS id
                FROM dbo.uv_usuario_perfil
                GROUP BY idUsuario
                ORDER BY COUNT(1) DESC, idUsuario
                """);
        assumeTrue(usuarioConPerfil.isPresent(), "No hay perfiles en dbo.uv_usuario_perfil para ejecutar la IT.");

        final List<UsuarioPerfilData> perfiles = authenticationRepositoryPort.consultarPerfilesPorUsuario(
                usuarioConPerfil.get()
        );
        final List<UsuarioPerfilData> perfilesVista = perfilesDesdeVista(usuarioConPerfil.get());

        assertFalse(perfiles.isEmpty());
        assertEquals(perfilesVista.size(), perfiles.size());
        for (int index = 0; index < perfilesVista.size(); index++) {
            assertPerfilEquals(perfilesVista.get(index), perfiles.get(index));
        }
    }

    private void assertPerfilEquals(final UsuarioPerfilData expected, final UsuarioPerfilData actual) {
        assertEquals(expected.getIdUsuario(), actual.getIdUsuario());
        assertEquals(expected.getIdPerfil(), actual.getIdPerfil());
        assertEquals(expected.getCodigoPerfil(), actual.getCodigoPerfil());
        assertEquals(expected.getNombrePerfil(), actual.getNombrePerfil());
        assertEquals(expected.getEstado(), actual.getEstado());
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }

    private List<UsuarioPerfilData> perfilesDesdeVista(final UUID idUsuario) {
        return jdbcTemplate.query(
                """
                        SELECT
                            idUsuario,
                            idPerfil,
                            codigoPerfil,
                            nombrePerfil,
                            estado
                        FROM dbo.uv_usuario_perfil
                        WHERE idUsuario = ?
                        ORDER BY codigoPerfil, idPerfil
                        """,
                (resultSet, rowNumber) -> new UsuarioPerfilData(
                        UUID.fromString(String.valueOf(resultSet.getObject("idUsuario"))),
                        UUID.fromString(String.valueOf(resultSet.getObject("idPerfil"))),
                        resultSet.getString("codigoPerfil"),
                        resultSet.getString("nombrePerfil"),
                        resultSet.getString("estado")
                ),
                idUsuario
        );
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
                "it.auth." + suffix.substring(0, 20) + "@uco.edu.co"
        );
    }

    private record TestIdentity(Integer numeroIdentificacion, String correo) {
    }
}
