package co.edu.uco.asistenciasuco.crosscutting.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextHelperUnirNombresTest {

    @Test
    void une_las_dos_partes_con_un_espacio() {
        assertEquals("Ana Maria", TextHelper.unirNombres("Ana", "Maria"));
        assertEquals("Ana Maria", TextHelper.unirNombres("  Ana ", " Maria  "));
    }

    @Test
    void un_segundo_nombre_ausente_no_se_rellena() {
        assertEquals("Ana", TextHelper.unirNombres("Ana", null));
        assertEquals("Ana", TextHelper.unirNombres("Ana", "   "));
        assertEquals("Maria", TextHelper.unirNombres(null, "Maria"));
    }

    @Test
    void sin_texto_en_ninguna_parte_devuelve_nulo() {
        assertNull(TextHelper.unirNombres(null, null));
        assertNull(TextHelper.unirNombres(" ", ""));
    }
}
