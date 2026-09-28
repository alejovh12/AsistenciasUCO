package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqlServerParameterCatalogAdapterTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private SqlServerParameterCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        adapter = new SqlServerParameterCatalogAdapter(jdbcTemplate);
    }

    private static SqlParameterSource anyParams() {
        return Mockito.any(SqlParameterSource.class);
    }

    private void stubValue(final String value) {
        when(jdbcTemplate.queryForObject(anyString(), anyParams(), eq(String.class))).thenReturn(value);
    }

    @Test
    void constructor_rechaza_template_nulo() {
        assertThrows(NullPointerException.class, () -> new SqlServerParameterCatalogAdapter(null));
    }

    @Test
    void getParameter_con_grupo_o_clave_nulos_o_en_blanco_devuelve_vacio_sin_consultar_bd() {
        assertTrue(adapter.getParameter(null, "k").isEmpty());
        assertTrue(adapter.getParameter(" ", "k").isEmpty());
        assertTrue(adapter.getParameter("g", null).isEmpty());
        assertTrue(adapter.getParameter("g", "").isEmpty());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void getParameter_consulta_con_grupo_y_clave_normalizados_y_cachea_el_resultado() {
        stubValue("15");

        assertEquals(Optional.of("15"), adapter.getParameter(" asistencia ", " ventana "));
        assertEquals(Optional.of("15"), adapter.getParameter("asistencia", "ventana"));

        final ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), params.capture(), eq(String.class));
        assertEquals("asistencia", params.getValue().getValue("grupo"));
        assertEquals("ventana", params.getValue().getValue("clave"));
    }

    @Test
    void getParameter_devuelve_vacio_cuando_no_hay_fila() {
        when(jdbcTemplate.queryForObject(anyString(), anyParams(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertTrue(adapter.getParameter("g", "k").isEmpty());
    }

    @Test
    void getParameter_devuelve_vacio_y_no_cachea_cuando_el_valor_es_nulo() {
        stubValue(null);

        assertTrue(adapter.getParameter("g", "k").isEmpty());
        assertTrue(adapter.getParameter("g", "k").isEmpty());

        verify(jdbcTemplate, times(2)).queryForObject(anyString(), anyParams(), eq(String.class));
    }

    @Test
    void getParameter_traduce_errores_de_bd_a_parameter_catalog_exception() {
        final IllegalStateException failure = new IllegalStateException("bd caida");
        when(jdbcTemplate.queryForObject(anyString(), anyParams(), eq(String.class))).thenThrow(failure);

        final ParameterCatalogException ex = assertThrows(ParameterCatalogException.class,
                () -> adapter.getParameter("g", "k"));

        assertTrue(ex.getMessage().contains("g:k"));
        assertSame(failure, ex.getCause());
    }

    @Test
    void getRequiredParameter_devuelve_el_valor_cuando_existe() {
        stubValue("valor");

        assertEquals("valor", adapter.getRequiredParameter("g", "k"));
    }

    @Test
    void getRequiredParameter_lanza_not_found_cuando_no_existe() {
        when(jdbcTemplate.queryForObject(anyString(), anyParams(), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        final ParameterNotFoundException ex = assertThrows(ParameterNotFoundException.class,
                () -> adapter.getRequiredParameter("g", "k"));

        assertTrue(ex.getMessage().contains("g:k"));
    }

    @Test
    void getParameterAs_convierte_a_string_sin_alterar_el_valor() {
        stubValue(" texto ");

        assertEquals(" texto ", adapter.getParameterAs("g", "k", String.class));
    }

    @Test
    void getParameterAs_convierte_a_entero_de_wrapper_y_primitivo() {
        stubValue(" 15 ");

        assertEquals(15, adapter.getParameterAs("g", "k", Integer.class));
        assertEquals(15, adapter.getParameterAs("g", "k", int.class));
    }

    @Test
    void getParameterAs_convierte_a_long_de_wrapper_y_primitivo() {
        stubValue("9000000000");

        assertEquals(9_000_000_000L, adapter.getParameterAs("g", "k", Long.class));
        assertEquals(9_000_000_000L, adapter.getParameterAs("g", "k", long.class));
    }

    @Test
    void getParameterAs_convierte_a_boolean_de_wrapper_y_primitivo() {
        stubValue(" true ");
        assertTrue(adapter.getParameterAs("g", "k", Boolean.class));
        assertTrue(adapter.getParameterAs("g", "k", boolean.class));

        adapter.clearCache();
        stubValue("otro");
        assertFalse(adapter.getParameterAs("g", "k", Boolean.class));
    }

    @Test
    void getParameterAs_convierte_a_double_de_wrapper_y_primitivo() {
        stubValue("2.5");

        assertEquals(2.5d, adapter.getParameterAs("g", "k", Double.class));
        assertEquals(2.5d, adapter.getParameterAs("g", "k", double.class));
    }

    @Test
    void getParameterAs_rechaza_tipos_destino_no_soportados() {
        stubValue("1");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapter.getParameterAs("g", "k", BigDecimal.class));

        assertTrue(ex.getMessage().contains("BigDecimal"));
    }

    @Test
    void getParameterAs_propaga_error_de_formato_numerico() {
        stubValue("no-numerico");

        assertThrows(NumberFormatException.class, () -> adapter.getParameterAs("g", "k", Integer.class));
    }

    @Test
    void clearCache_fuerza_nueva_consulta() {
        stubValue("valor");
        adapter.getParameter("g", "k");

        adapter.clearCache();
        adapter.getParameter("g", "k");

        verify(jdbcTemplate, times(2)).queryForObject(anyString(), anyParams(), eq(String.class));
    }
}
