package co.edu.uco.asistenciasuco.crosscutting.util;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextHelperTest {

    @Test
    void trim_con_valor_nulo_retorna_nulo() {
        assertNull(TextHelper.trim(null));
    }

    @Test
    void trim_con_valor_con_espacios_los_elimina() {
        assertEquals("hola", TextHelper.trim("  hola  "));
    }

    @Test
    void trimToEmpty_con_valor_nulo_retorna_vacio() {
        assertEquals("", TextHelper.trimToEmpty(null));
    }

    @Test
    void trimToEmpty_con_valor_valido_lo_recorta() {
        assertEquals("hola", TextHelper.trimToEmpty("  hola  "));
    }

    @Test
    void normalizeTrimUpper_con_valor_nulo_retorna_nulo() {
        assertNull(TextHelper.normalizeTrimUpper(null));
    }

    @Test
    void normalizeTrimUpper_con_valor_valido_lo_normaliza() {
        assertEquals("HOLA", TextHelper.normalizeTrimUpper("  hola  "));
    }

    @Test
    void normalizeTrimUpperToEmpty_con_valor_nulo_retorna_vacio() {
        assertEquals("", TextHelper.normalizeTrimUpperToEmpty(null));
    }

    @Test
    void normalizeTrimUpperToEmpty_con_valor_valido_lo_normaliza() {
        assertEquals("HOLA", TextHelper.normalizeTrimUpperToEmpty("  hola  "));
    }

    @Test
    void normalizeTrimLower_con_valor_nulo_retorna_nulo() {
        assertNull(TextHelper.normalizeTrimLower(null));
    }

    @Test
    void normalizeTrimLower_con_valor_valido_lo_normaliza() {
        assertEquals("hola", TextHelper.normalizeTrimLower("  HOLA  "));
    }

    @Test
    void isNull_distingue_valores_nulos_y_no_nulos() {
        assertTrue(TextHelper.isNull(null));
        assertFalse(TextHelper.isNull("hola"));
    }

    @Test
    void isBlank_distingue_blanco_nulo_y_con_contenido() {
        assertFalse(TextHelper.isBlank(null));
        assertTrue(TextHelper.isBlank("   "));
        assertFalse(TextHelper.isBlank("hola"));
    }

    @Test
    void isNullOrBlank_cubre_nulo_blanco_y_valido() {
        assertTrue(TextHelper.isNullOrBlank(null));
        assertTrue(TextHelper.isNullOrBlank("   "));
        assertFalse(TextHelper.isNullOrBlank("hola"));
    }

    @Test
    void hasLengthBetween_con_rango_invalido_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> TextHelper.hasLengthBetween("hola", -1, 5));
        assertThrows(CrosscuttingException.class, () -> TextHelper.hasLengthBetween("hola", 5, 1));
    }

    @Test
    void hasLengthBetween_con_valor_nulo_retorna_falso() {
        assertFalse(TextHelper.hasLengthBetween(null, 1, 5));
    }

    @Test
    void hasLengthBetween_evalua_limites_correctamente() {
        assertTrue(TextHelper.hasLengthBetween("hola", 1, 5));
        assertFalse(TextHelper.hasLengthBetween("hola", 5, 10));
        assertFalse(TextHelper.hasLengthBetween("hola mundo", 1, 5));
    }

    @Test
    void requireNonBlank_con_valor_en_blanco_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> TextHelper.requireNonBlank("   ", "mensaje"));
    }

    @Test
    void requireNonBlank_con_valor_valido_lo_retorna() {
        assertEquals("hola", TextHelper.requireNonBlank("hola", "mensaje"));
    }

    @Test
    void requireMaxLength_con_maximo_negativo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> TextHelper.requireMaxLength("hola", -1, "mensaje"));
    }

    @Test
    void requireMaxLength_con_valor_excedido_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> TextHelper.requireMaxLength("hola mundo", 3, "mensaje"));
    }

    @Test
    void requireMaxLength_con_valor_nulo_o_valido_lo_retorna() {
        assertNull(TextHelper.requireMaxLength(null, 5, "mensaje"));
        assertEquals("hola", TextHelper.requireMaxLength("hola", 10, "mensaje"));
    }

    @Test
    void requireLengthBetween_fuera_de_rango_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> TextHelper.requireLengthBetween("hola", 5, 10, "mensaje"));
    }

    @Test
    void requireLengthBetween_dentro_de_rango_lo_retorna() {
        assertEquals("hola", TextHelper.requireLengthBetween("hola", 1, 5, "mensaje"));
    }

    @Test
    void requireEmailFormat_con_formato_invalido_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> TextHelper.requireEmailFormat("no-es-correo", "mensaje"));
        assertThrows(IllegalArgumentException.class, () -> TextHelper.requireEmailFormat("", "mensaje"));
    }

    @Test
    void requireEmailFormat_con_correo_valido_lo_retorna() {
        assertEquals("ana@uco.edu.co", TextHelper.requireEmailFormat("ana@uco.edu.co", "mensaje"));
    }

    @Test
    void containsWhitespace_detecta_espacios() {
        assertFalse(TextHelper.containsWhitespace(null));
        assertTrue(TextHelper.containsWhitespace("hola mundo"));
        assertFalse(TextHelper.containsWhitespace("hola"));
    }

    @Test
    void isNotNull_distingue_valores_nulos_y_no_nulos() {
        assertFalse(TextHelper.isNotNull(null));
        assertTrue(TextHelper.isNotNull("hola"));
    }
}
