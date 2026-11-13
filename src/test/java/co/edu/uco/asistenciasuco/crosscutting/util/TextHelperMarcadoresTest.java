package co.edu.uco.asistenciasuco.crosscutting.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** VAL-oleada3 MSG-05: llenado de los marcadores {@code {}} de los mensajes de catalogo. */
class TextHelperMarcadoresTest {

    @Test
    void llena_los_marcadores_en_orden() {
        assertEquals("El campo fecha debe usar formato ISO local yyyy-MM-dd.",
                TextHelper.reemplazarMarcadores("El campo {} debe usar formato ISO local {}.", "fecha", "yyyy-MM-dd"));
    }

    @Test
    void sin_argumentos_devuelve_la_plantilla() {
        assertEquals("El {} especificado.", TextHelper.reemplazarMarcadores("El {} especificado."));
        assertEquals("El {} especificado.", TextHelper.reemplazarMarcadores("El {} especificado.", (Object[]) null));
    }

    @Test
    void los_marcadores_sobrantes_quedan_y_los_argumentos_sobrantes_se_ignoran() {
        assertEquals("A 1 y {}", TextHelper.reemplazarMarcadores("A {} y {}", 1));
        assertEquals("Sin marcador", TextHelper.reemplazarMarcadores("Sin marcador", "x"));
    }

    @Test
    void plantilla_nula_devuelve_cadena_vacia() {
        assertEquals("", TextHelper.reemplazarMarcadores(null, "x"));
    }
}
