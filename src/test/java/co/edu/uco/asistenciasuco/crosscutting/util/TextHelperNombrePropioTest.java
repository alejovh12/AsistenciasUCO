package co.edu.uco.asistenciasuco.crosscutting.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** IDENT-001: los nombres llegan normalizados en mayusculas; el correo los muestra como nombre propio. */
class TextHelperNombrePropioTest {

    @Test
    void convierte_a_formato_de_nombre_propio() {
        assertEquals("Ana", TextHelper.toNombrePropio("ANA"));
        assertEquals("Ana Maria", TextHelper.toNombrePropio("ANA MARIA"));
        assertEquals("Ana Maria", TextHelper.toNombrePropio("  ana   maria  "));
        assertEquals("José", TextHelper.toNombrePropio("JOSÉ"));
    }

    @Test
    void nulo_o_vacio_devuelve_cadena_vacia() {
        assertEquals("", TextHelper.toNombrePropio(null));
        assertEquals("", TextHelper.toNombrePropio(""));
        assertEquals("", TextHelper.toNombrePropio("   "));
    }
}
