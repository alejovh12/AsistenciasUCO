package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DEC-073 (migracion 42): las reglas de formularios que tambien valida la base de datos llegan como 400 con su propio codigo,
 * no como error tecnico. VCE_008, ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA y ASI_008 conservan el mensaje que resolvio la BD
 * (dominio, limites y maximo vigentes); SES_028 y ERR_CODIGO_GRUPO_INVALIDO usan su texto institucional.
 */
class DbExceptionTranslatorDec073Test {

    private static ValidationException fallo(final String dbCode, final String mensajeDb) {
        return assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensajeDb, "DBCODE=" + dbCode + "|detalle tecnico", "corr", "operacionDec073"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "VCE_008;El correo debe ser institucional y terminar en @uco.edu.co.",
            "ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA;Al rechazar un reclamo la respuesta debe tener entre 10 y 500 caracteres.",
            "ASI_008;Los créditos de la asignatura no pueden superar 10."
    })
    void los_codigos_con_parametros_conservan_el_mensaje_de_la_base_de_datos(final String codigo, final String mensajeDb) {
        final ValidationException exception = fallo(codigo, mensajeDb);

        assertEquals(codigo, exception.getCode());
        assertEquals(mensajeDb, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SES_028", "ERR_CODIGO_GRUPO_INVALIDO"})
    void los_codigos_sin_parametros_son_validacion_con_su_codigo(final String codigo) {
        assertEquals(codigo, fallo(codigo, "Mensaje publico.").getCode());
    }
}
