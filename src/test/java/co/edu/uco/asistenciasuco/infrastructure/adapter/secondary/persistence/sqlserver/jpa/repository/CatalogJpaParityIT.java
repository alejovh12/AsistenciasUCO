package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.MessageCatalogJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.ParameterCatalogJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.CatalogJdbcBaseline;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Paridad JPA-06 de catalogos contra SQL Server real (LB-008 JPA-06).
 *
 * <p>Compara BEFORE (oraculo JDBC sobre tablas base con NOLOCK, solo src/test) contra AFTER (JPA sobre
 * {@code uv_mensaje_usuario}, {@code uv_mensaje_tecnico} y {@code uv_parametro}). Cubre coincidencia por codigo,
 * registro inexistente, registro inactivo, datos vivos y semantica de cache (hit y miss). Los fixtures usan codigos
 * con prefijo unico y se eliminan en {@code finally}.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class CatalogJpaParityIT {

    private static final String PREFIJO = "IT-LB008-JPA06-";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void mensaje_de_usuario_activo_coincide_entre_jdbc_before_y_jpa_after() {
        final String codigo = PREFIJO + "U-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarMensajeUsuario(id, codigo, "Mensaje de usuario de paridad", 1);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(before.buscarMensajeUsuario(codigo), after.buscarMensajeUsuario(codigo));
            assertEquals(Optional.of("Mensaje de usuario de paridad"), after.buscarMensajeUsuario(codigo));
        } finally {
            eliminarMensajeUsuario(id);
        }
    }

    @Test
    void mensaje_de_usuario_inactivo_no_se_resuelve_en_ninguno_de_los_dos_lados() {
        final String codigo = PREFIJO + "UI-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarMensajeUsuario(id, codigo, "Mensaje inactivo", 0);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(Optional.empty(), before.buscarMensajeUsuario(codigo));
            assertEquals(Optional.empty(), after.buscarMensajeUsuario(codigo));
        } finally {
            eliminarMensajeUsuario(id);
        }
    }

    @Test
    void mensaje_de_usuario_inexistente_devuelve_vacio_en_ambos_lados() {
        final String codigo = PREFIJO + "UN-" + UUID.randomUUID();
        final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
        final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

        assertEquals(Optional.empty(), before.buscarMensajeUsuario(codigo));
        assertEquals(Optional.empty(), after.buscarMensajeUsuario(codigo));
    }

    @Test
    void mensaje_de_usuario_vivo_coincide_entre_jdbc_before_y_jpa_after() {
        final List<String> codigos = jdbc.queryForList(
                "SELECT TOP 1 codigo FROM dbo.uv_mensaje_usuario ORDER BY codigo", String.class);
        assumeTrue(!codigos.isEmpty(), "Se requiere al menos un mensaje de usuario vivo.");

        final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
        final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

        final Optional<String> esperado = before.buscarMensajeUsuario(codigos.getFirst());
        assertTrue(esperado.isPresent(), "El codigo vivo debe resolverse en el baseline JDBC.");
        assertEquals(esperado, after.buscarMensajeUsuario(codigos.getFirst()));
    }

    @Test
    void mensaje_tecnico_activo_coincide_entre_jdbc_before_y_jpa_after() {
        final String codigo = PREFIJO + "T-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarMensajeTecnico(id, codigo, "Detalle tecnico de paridad", 1);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(before.buscarMensajeTecnico(codigo), after.buscarMensajeTecnico(codigo));
            assertEquals(Optional.of("Detalle tecnico de paridad"), after.buscarMensajeTecnico(codigo));
        } finally {
            eliminarMensajeTecnico(id);
        }
    }

    @Test
    void mensaje_tecnico_inactivo_no_se_resuelve_en_ninguno_de_los_dos_lados() {
        final String codigo = PREFIJO + "TI-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarMensajeTecnico(id, codigo, "Detalle inactivo", 0);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(Optional.empty(), before.buscarMensajeTecnico(codigo));
            assertEquals(Optional.empty(), after.buscarMensajeTecnico(codigo));
        } finally {
            eliminarMensajeTecnico(id);
        }
    }

    @Test
    void parametro_activo_coincide_entre_jdbc_before_y_jpa_after() {
        final String grupo = PREFIJO + "G";
        final String clave = "CLAVE-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarParametro(id, grupo, clave, "valor-paridad", 1);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(before.buscarParametro(grupo, clave), after.buscarParametro(grupo, clave));
            assertEquals(Optional.of("valor-paridad"), after.buscarParametro(grupo, clave));
        } finally {
            eliminarParametro(id);
        }
    }

    @Test
    void parametro_inactivo_o_inexistente_devuelve_vacio_en_ambos_lados() {
        final String grupo = PREFIJO + "GI";
        final String clave = "CLAVE-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarParametro(id, grupo, clave, "inactivo", 0);
        try {
            final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
            final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

            assertEquals(Optional.empty(), before.buscarParametro(grupo, clave));
            assertEquals(Optional.empty(), after.buscarParametro(grupo, clave));
            assertEquals(Optional.empty(), after.buscarParametro(grupo, PREFIJO + "NO-EXISTE"));
        } finally {
            eliminarParametro(id);
        }
    }

    @Test
    void parametro_vivo_coincide_entre_jdbc_before_y_jpa_after() {
        final List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT TOP 1 grupo, clave FROM dbo.uv_parametro ORDER BY grupo, clave");
        assumeTrue(!filas.isEmpty(), "Se requiere al menos un parametro vivo.");

        final String grupo = String.valueOf(filas.getFirst().get("grupo"));
        final String clave = String.valueOf(filas.getFirst().get("clave"));
        final CatalogJdbcBaseline before = new CatalogJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
        final CatalogJpaParityHarness after = new CatalogJpaParityHarness(entityManager);

        assertEquals(before.buscarParametro(grupo, clave), after.buscarParametro(grupo, clave));
    }

    @Test
    void cache_del_adapter_de_mensajes_mantiene_hit_y_miss_tras_jpa() {
        final String codigo = PREFIJO + "C-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        insertarMensajeUsuario(id, codigo, "contenido original", 1);
        try {
            final MessageCatalogJpaRepository adapter = new MessageCatalogJpaRepository(entityManager);

            assertEquals(Optional.of("contenido original"), adapter.findUserMessage(codigo));

            jdbc.update("UPDATE dbo.CatalogoMensajeUsuario SET contenido = ? WHERE id = ?", "contenido cambiado", id);
            assertEquals(Optional.of("contenido original"), adapter.findUserMessage(codigo),
                    "Cache hit: el valor cacheado no se vuelve a consultar.");

            // La cache es por instancia y no existe invalidacion de produccion: una instancia nueva no tiene cache
            // y observa el valor vivo; la original conserva su cache.
            final MessageCatalogJpaRepository adapterSinCache = new MessageCatalogJpaRepository(entityManager);
            assertEquals(Optional.of("contenido cambiado"), adapterSinCache.findUserMessage(codigo),
                    "Instancia sin cache: se consulta de nuevo la vista.");
            assertEquals(Optional.of("contenido original"), adapter.findUserMessage(codigo),
                    "La cache de la instancia original no se altera por otra instancia.");
        } finally {
            eliminarMensajeUsuario(id);
        }
    }

    @Test
    void cache_del_adapter_de_parametros_no_cachea_inexistentes_y_expone_el_valor_vivo() {
        final String grupo = PREFIJO + "CG";
        final String clave = "CLAVE-" + UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ParameterCatalogJpaRepository adapter = new ParameterCatalogJpaRepository(entityManager);
        try {
            assertEquals(Optional.empty(), adapter.getParameter(grupo, clave));

            insertarParametro(id, grupo, clave, "aparece-despues", 1);
            assertEquals(Optional.of("aparece-despues"), adapter.getParameter(grupo, clave),
                    "Un parametro inexistente no se cachea: al aparecer, se resuelve.");
        } finally {
            eliminarParametro(id);
        }
    }

    private void insertarMensajeUsuario(final UUID id, final String codigo, final String contenido, final int activo) {
        jdbc.update("""
                INSERT INTO dbo.CatalogoMensajeUsuario (id, codigo, tipoMensaje, severidad, contenido, estaActivo)
                VALUES (?, ?, 'BUSINESS_ERROR', 'MEDIO', ?, ?)
                """, id, codigo, contenido, activo);
    }

    private void insertarMensajeTecnico(final UUID id, final String codigo, final String contenido, final int activo) {
        jdbc.update("""
                INSERT INTO dbo.CatalogoMensajeTecnico (id, codigo, tipoMensaje, severidad, contenido, estaActivo)
                VALUES (?, ?, 'TECHNICAL_ERROR', 'ALTO', ?, ?)
                """, id, codigo, contenido, activo);
    }

    private void insertarParametro(final UUID id, final String grupo, final String clave, final String valor,
                                   final int activo) {
        jdbc.update("""
                INSERT INTO dbo.CatalogoParametro (id, grupo, clave, valor, tipoDato, valorDefecto, estaActivo)
                VALUES (?, ?, ?, ?, 'STRING', '', ?)
                """, id, grupo, clave, valor, activo);
    }

    private void eliminarMensajeUsuario(final UUID id) {
        jdbc.update("DELETE FROM dbo.CatalogoMensajeUsuario WHERE id = ?", id);
    }

    private void eliminarMensajeTecnico(final UUID id) {
        jdbc.update("DELETE FROM dbo.CatalogoMensajeTecnico WHERE id = ?", id);
    }

    private void eliminarParametro(final UUID id) {
        jdbc.update("DELETE FROM dbo.CatalogoParametro WHERE id = ?", id);
    }
}




