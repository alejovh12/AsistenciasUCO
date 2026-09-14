package co.edu.uco.asistenciasuco.crosscutting.helpers;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberHelperTest {

    @Test
    void requirePositive_con_valor_nulo_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> NumberHelper.requirePositive(null, "nulo", "no positivo"));
    }

    @Test
    void requirePositive_con_valor_cero_o_negativo_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> NumberHelper.requirePositive(0, "nulo", "no positivo"));
        assertThrows(IllegalArgumentException.class, () -> NumberHelper.requirePositive(-5, "nulo", "no positivo"));
    }

    @Test
    void requirePositive_con_valor_positivo_lo_retorna() {
        assertEquals(5, NumberHelper.requirePositive(5, "nulo", "no positivo"));
    }

    @Test
    void requireDigitLengthBetween_con_rango_invalido_lanza_excepcion() {
        assertThrows(CrosscuttingException.class,
                () -> NumberHelper.requireDigitLengthBetween(123, -1, 5, "mensaje"));
        assertThrows(CrosscuttingException.class,
                () -> NumberHelper.requireDigitLengthBetween(123, 5, 1, "mensaje"));
    }

    @Test
    void requireDigitLengthBetween_con_valor_nulo_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberHelper.requireDigitLengthBetween(null, 1, 5, "mensaje"));
    }

    @Test
    void requireDigitLengthBetween_fuera_de_rango_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberHelper.requireDigitLengthBetween(1, 3, 5, "mensaje"));
        assertThrows(IllegalArgumentException.class,
                () -> NumberHelper.requireDigitLengthBetween(123456, 3, 5, "mensaje"));
    }

    @Test
    void requireDigitLengthBetween_dentro_de_rango_retorna_valor() {
        assertEquals(12345, NumberHelper.requireDigitLengthBetween(12345, 3, 5, "mensaje"));
    }
}
