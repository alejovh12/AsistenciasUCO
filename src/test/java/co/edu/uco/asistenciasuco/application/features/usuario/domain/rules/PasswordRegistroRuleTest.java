package co.edu.uco.asistenciasuco.application.features.usuario.domain.rules;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordRegistroRuleTest {

    private final Integer numeroIdentificacion = 123456789;

    @Test
    void resolver_con_password_null_representa_credencial_no_generada() {
        final String password = PasswordRegistroRule.resolver(null, numeroIdentificacion);

        assertNull(password);
    }

    @Test
    void resolver_con_password_igual_a_identificacion_rechaza_fallback_inseguro() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> PasswordRegistroRule.resolver("123456789", numeroIdentificacion)
        );

        assertEquals("ERR_PASSWORD_IGUAL_IDENTIFICACION", exception.getCode());
    }

    @Test
    void resolver_con_password_explicito_valido_crea_password_explicito() {
        final String password = PasswordRegistroRule.resolver("Clave123!", numeroIdentificacion);

        assertEquals("Clave123!", password);
    }

    @Test
    void resolver_acepta_solo_caracteres_especiales_compatibles_con_sql() {
        assertEquals("Clave123%", PasswordRegistroRule.resolver("Clave123%", numeroIdentificacion));

        assertPasswordInvalido("Clave123?", "ERR_PASSWORD_POLITICA_INVALIDA");
    }

    @Test
    void resolver_rechaza_password_explicito_invalido_vacio_y_con_espacios_con_codigo_semantico() {
        assertPasswordInvalido("", "ERR_PASSWORD_POLITICA_INVALIDA");
        assertPasswordInvalido("sinreglas", "ERR_PASSWORD_POLITICA_INVALIDA");
        assertPasswordInvalido(" Clave123! ", "ERR_PASSWORD_POLITICA_INVALIDA");
        assertPasswordInvalido("A".repeat(256), "ERR_PASSWORD_LONGITUD_INVALIDA");
    }

    @Test
    void resolver_rechaza_numero_identificacion_null_con_validationException() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> PasswordRegistroRule.resolver("Clave123!", null)
        );

        assertEquals("ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO", exception.getCode());
    }

    @Test
    void resolverCredencialNueva_rechaza_password_null() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> PasswordRegistroRule.resolverCredencialNueva(null, numeroIdentificacion)
        );

        assertEquals("ERR_PASSWORD_REQUERIDO", exception.getCode());
    }

    private void assertPasswordInvalido(final String password, final String expectedCode) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> PasswordRegistroRule.resolver(password, numeroIdentificacion)
        );

        assertEquals(expectedCode, exception.getCode());
    }
}
