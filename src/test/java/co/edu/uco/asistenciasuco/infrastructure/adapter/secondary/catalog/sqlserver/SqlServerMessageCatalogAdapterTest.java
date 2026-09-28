package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort.MessageCatalogException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqlServerMessageCatalogAdapterTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private SqlServerMessageCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        adapter = new SqlServerMessageCatalogAdapter(jdbcTemplate);
    }

    private void stubUserQuery(final String value) {
        when(jdbcTemplate.queryForObject(contains("CatalogoMensajeUsuario"), any(), eq(String.class))).thenReturn(value);
    }

    private void stubTechnicalQuery(final String value) {
        when(jdbcTemplate.queryForObject(contains("CatalogoMensajeTecnico"), any(), eq(String.class))).thenReturn(value);
    }

    private static SqlParameterSource any() {
        return Mockito.any(SqlParameterSource.class);
    }

    @Test
    void constructor_rechaza_template_nulo() {
        assertThrows(NullPointerException.class, () -> new SqlServerMessageCatalogAdapter(null));
    }

    // ------------------------------------------------------------------ findUserMessage

    @Test
    void findUserMessage_con_codigo_nulo_o_en_blanco_devuelve_vacio_sin_consultar_bd() {
        assertTrue(adapter.findUserMessage(null).isEmpty());
        assertTrue(adapter.findUserMessage("").isEmpty());
        assertTrue(adapter.findUserMessage("  ").isEmpty());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void findUserMessage_consulta_con_codigo_normalizado_y_cachea_el_resultado() {
        stubUserQuery("Campo obligatorio");

        assertEquals(Optional.of("Campo obligatorio"), adapter.findUserMessage("  VAL-001 "));
        assertEquals(Optional.of("Campo obligatorio"), adapter.findUserMessage("VAL-001"));

        final ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), params.capture(), eq(String.class));
        assertEquals("VAL-001", params.getValue().getValue("codigo"));
    }

    @Test
    void findUserMessage_devuelve_vacio_cuando_la_bd_no_tiene_fila() {
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());
    }

    @Test
    void findUserMessage_devuelve_vacio_y_no_cachea_cuando_el_contenido_es_nulo() {
        stubUserQuery(null);

        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());
        assertTrue(adapter.findUserMessage("VAL-001").isEmpty());

        verify(jdbcTemplate, times(2)).queryForObject(anyString(), any(), eq(String.class));
    }

    @Test
    void findUserMessage_traduce_errores_de_bd_a_message_catalog_exception() {
        final IllegalStateException failure = new IllegalStateException("bd caida");
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class))).thenThrow(failure);

        final MessageCatalogException ex = assertThrows(MessageCatalogException.class,
                () -> adapter.findUserMessage("VAL-001"));

        assertTrue(ex.getMessage().contains("VAL-001"));
        assertSame(failure, ex.getCause());
    }

    // ------------------------------------------------------------------ findTechnicalMessage

    @Test
    void findTechnicalMessage_con_codigo_nulo_o_en_blanco_devuelve_vacio_sin_consultar_bd() {
        assertTrue(adapter.findTechnicalMessage(null).isEmpty());
        assertTrue(adapter.findTechnicalMessage(" ").isEmpty());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void findTechnicalMessage_consulta_la_tabla_tecnica_y_cachea_el_resultado() {
        stubTechnicalQuery("detalle tecnico");

        assertEquals(Optional.of("detalle tecnico"), adapter.findTechnicalMessage(" VAL-001 "));
        assertEquals(Optional.of("detalle tecnico"), adapter.findTechnicalMessage("VAL-001"));

        verify(jdbcTemplate, times(1)).queryForObject(contains("CatalogoMensajeTecnico"), any(), eq(String.class));
    }

    @Test
    void findTechnicalMessage_devuelve_vacio_cuando_no_hay_fila_o_contenido() {
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertTrue(adapter.findTechnicalMessage("VAL-001").isEmpty());

        Mockito.reset(jdbcTemplate);
        stubTechnicalQuery(null);
        assertTrue(adapter.findTechnicalMessage("VAL-001").isEmpty());
    }

    @Test
    void findTechnicalMessage_traduce_errores_de_bd() {
        final IllegalStateException failure = new IllegalStateException("bd caida");
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class))).thenThrow(failure);

        final MessageCatalogException ex = assertThrows(MessageCatalogException.class,
                () -> adapter.findTechnicalMessage("VAL-001"));

        assertTrue(ex.getMessage().contains("VAL-001"));
        assertSame(failure, ex.getCause());
    }

    // ------------------------------------------------------------------ getUserMessage / getTechnicalMessage

    @Test
    void getUserMessage_formatea_la_plantilla_con_los_argumentos() {
        stubUserQuery("El campo {0} debe tener {1} caracteres");

        assertEquals("El campo nombre debe tener 5 caracteres", adapter.getUserMessage("VAL-001", "nombre", 5));
    }

    @Test
    void getUserMessage_sin_argumentos_o_con_argumentos_nulos_devuelve_la_plantilla() {
        stubUserQuery("Valor {0} pendiente");

        assertEquals("Valor {0} pendiente", adapter.getUserMessage("VAL-001"));
        assertEquals("Valor {0} pendiente", adapter.getUserMessage("VAL-001", (Object[]) null));
    }

    @Test
    void getUserMessage_devuelve_el_codigo_cuando_no_existe_mensaje() {
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertEquals("VAL-001", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getUserMessage_devuelve_la_plantilla_cuando_el_patron_es_invalido() {
        stubUserQuery("Patron roto {0");

        assertEquals("Patron roto {0", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getUserMessage_con_plantilla_en_blanco_no_formatea() {
        stubUserQuery("  ");

        assertEquals("  ", adapter.getUserMessage("VAL-001", "x"));
    }

    @Test
    void getTechnicalMessage_formatea_el_mensaje_tecnico() {
        stubTechnicalQuery("Fallo en {0}");

        assertEquals("Fallo en modulo", adapter.getTechnicalMessage("VAL-001", "modulo"));
    }

    @Test
    void getTechnicalMessage_devuelve_el_codigo_cuando_no_existe_mensaje() {
        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertEquals("VAL-001", adapter.getTechnicalMessage("VAL-001", "modulo"));
    }

    // ------------------------------------------------------------------ cache

    @Test
    void clearCache_fuerza_nueva_consulta_para_mensajes_de_usuario_y_tecnicos() {
        stubUserQuery("usuario");
        stubTechnicalQuery("tecnico");
        adapter.findUserMessage("VAL-001");
        adapter.findTechnicalMessage("VAL-001");

        adapter.clearCache();
        adapter.findUserMessage("VAL-001");
        adapter.findTechnicalMessage("VAL-001");

        verify(jdbcTemplate, times(2)).queryForObject(contains("CatalogoMensajeUsuario"), any(), eq(String.class));
        verify(jdbcTemplate, times(2)).queryForObject(contains("CatalogoMensajeTecnico"), any(), eq(String.class));
    }
}
