package co.edu.uco.asistenciasuco.crosscutting.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEC-073: texto seguro para nombres de entidades, texto libre sin HTML y numero de identificacion como texto. */
class ValidationHelperDec073Test {

    @ParameterizedTest
    @ValueSource(strings = {"Grupo 001", "Ingeniería de Sistemas", "2027-1", "Cálculo (I)", "Sesión #3: repaso", "Lab/Taller_2", "Ñandú"})
    void texto_seguro_acepta_letras_numeros_y_la_puntuacion_permitida(final String valor) {
        assertTrue(ValidationHelper.isSafeText(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>alert(1)</script>", "Grupo <b>", "a > b", "Comillas \"dobles\"", "Apóstrofe '", "Tab\tdentro", "Punto y coma;"})
    void texto_seguro_rechaza_html_comillas_y_control(final String valor) {
        assertFalse(ValidationHelper.isSafeText(valor));
    }

    @Test
    void texto_seguro_deja_la_obligatoriedad_a_otra_regla() {
        assertTrue(ValidationHelper.isSafeText(null));
        assertTrue(ValidationHelper.isSafeText("   "));
    }

    @Test
    void texto_libre_solo_rechaza_los_delimitadores_de_etiquetas() {
        assertTrue(ValidationHelper.isWithoutHtmlTags("No presentó soporte; revisé el caso \"a fondo\"."));
        assertTrue(ValidationHelper.isWithoutHtmlTags(null));
        assertFalse(ValidationHelper.isWithoutHtmlTags("<b>hola</b>"));
        assertFalse(ValidationHelper.isWithoutHtmlTags("2 > 1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1017334455", "2147483647", " 123456 "})
    void numero_de_identificacion_como_texto_acepta_digitos_dentro_del_int(final String valor) {
        assertTrue(ValidationHelper.isIdentificationNumberText(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a45", "-5", "0", "0123", "2147483648", "99999999999", "12.5"})
    void numero_de_identificacion_como_texto_rechaza_letras_signos_ceros_iniciales_y_desbordes(final String valor) {
        assertFalse(ValidationHelper.isIdentificationNumberText(valor));
    }
}
