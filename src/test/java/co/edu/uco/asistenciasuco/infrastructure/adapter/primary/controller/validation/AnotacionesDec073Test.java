package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEC-073: las anotaciones delegan en ValidationHelper; nulo es valido (la obligatoriedad la declara @NotBlank). */
class AnotacionesDec073Test {

    @Test
    void nombre_persona_rechaza_numeros_y_html() {
        final NombrePersona.Validator v = new NombrePersona.Validator();
        assertTrue(v.isValid("José María", null));
        assertTrue(v.isValid("O'Neil", null));
        assertTrue(v.isValid(null, null));
        assertFalse(v.isValid("Juan123", null));
        assertFalse(v.isValid("12345", null));
        assertFalse(v.isValid("<b>Ana</b>", null));
    }

    @Test
    void texto_seguro_rechaza_html() {
        final TextoSeguro.Validator v = new TextoSeguro.Validator();
        assertTrue(v.isValid("Grupo 001", null));
        assertFalse(v.isValid("<script>alert(1)</script>", null));
    }

    @Test
    void sin_etiquetas_html_rechaza_menor_y_mayor() {
        final SinEtiquetasHtml.Validator v = new SinEtiquetasHtml.Validator();
        assertTrue(v.isValid("Respuesta normal.", null));
        assertFalse(v.isValid("<i>x</i>", null));
    }

    @Test
    void numero_identificacion_texto_rechaza_letras_y_desborde() {
        final NumeroIdentificacionTexto.Validator v = new NumeroIdentificacionTexto.Validator();
        assertTrue(v.isValid("1017334455", null));
        assertFalse(v.isValid("12a45", null));
        assertFalse(v.isValid("9999999999", null));
    }
}
