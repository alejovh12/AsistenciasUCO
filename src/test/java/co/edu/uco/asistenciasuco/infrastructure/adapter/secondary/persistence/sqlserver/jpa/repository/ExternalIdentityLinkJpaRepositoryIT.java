package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SEC-BASELINE-017 contra SQL Server real: el vínculo {@code issuer + subject → Usuario.id} se aplica en la DB
 * (índices únicos y SP canónicos), no solo en memoria. Cada ejecución usa un emisor aleatorio para no colisionar,
 * pero las filas de vínculo no se pueden borrar con el rol runtime (se conservan revocadas); por eso la prueba solo
 * corre con {@code EXTERNAL_IDENTITY_IT_ALLOW_WRITES=true} sobre una base desechable y se declara SKIP si no.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class ExternalIdentityLinkJpaRepositoryIT {

    @Autowired
    private ExternalIdentityLinkPort links;

    @Autowired
    private JdbcTemplate jdbc;

    private final String issuer = "http://it.invalid/realms/" + UUID.randomUUID();
    private UUID userA;
    private UUID userB;

    @BeforeEach
    void prepare() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("EXTERNAL_IDENTITY_IT_ALLOW_WRITES")),
                "Escribe vínculos permanentes; exige EXTERNAL_IDENTITY_IT_ALLOW_WRITES=true y una base desechable.");
        final List<UUID> users = jdbc.query(
                "SELECT TOP 2 id FROM dbo.uv_usuario WHERE estaActivoUsuario = 1 ORDER BY id",
                (rs, row) -> rs.getObject(1, UUID.class));
        assumeTrue(users.size() == 2, "Se necesitan dos usuarios activos en la base.");
        userA = users.get(0);
        userB = users.get(1);
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void clear() {
        CorrelationIdContext.clear();
    }

    @Test
    void primera_vinculacion_repeticion_idempotente_y_lectura_por_emisor_y_sujeto() {
        assertTrue(links.find(issuer, "sub-a").isEmpty());

        assertTrue(links.link(issuer, "sub-a", userA, "persona@uco.edu.co"));
        assertFalse(links.link(issuer, "sub-a", userA, "otro-correo@uco.edu.co"));

        final var link = links.find(issuer, "sub-a").orElseThrow();
        assertEquals(userA, link.usuarioId());
        assertTrue(link.vigente());
    }

    @Test
    void dos_identidades_no_ocupan_la_misma_cuenta_y_una_identidad_no_ocupa_dos_cuentas() {
        links.link(issuer, "sub-a", userA, "a@uco.edu.co");

        assertThrows(ExternalIdentityConflictException.class,
                () -> links.link(issuer, "sub-a-intruso", userA, "a@uco.edu.co"));
        assertThrows(ExternalIdentityConflictException.class,
                () -> links.link(issuer, "sub-a", userB, "b@uco.edu.co"));
        assertTrue(links.find(issuer, "sub-a-intruso").isEmpty());
        assertEquals(userA, links.find(issuer, "sub-a").orElseThrow().usuarioId());
    }

    @Test
    void la_revocacion_se_conserva_y_la_identidad_revocada_no_se_reutiliza() {
        links.link(issuer, "sub-a", userA, "a@uco.edu.co");

        links.revoke(issuer, "sub-a");
        links.revoke(issuer, "sub-a");

        assertFalse(links.find(issuer, "sub-a").orElseThrow().vigente());
        assertThrows(ExternalIdentityConflictException.class,
                () -> links.link(issuer, "sub-a", userA, "a@uco.edu.co"));
        assertThrows(ExternalIdentityConflictException.class,
                () -> links.link(issuer, "sub-a", userB, "b@uco.edu.co"));
        // Tras revocar, la cuenta puede recibir una identidad NUEVA del mismo emisor.
        assertTrue(links.link(issuer, "sub-a-nueva", userA, "a@uco.edu.co"));
    }
}
