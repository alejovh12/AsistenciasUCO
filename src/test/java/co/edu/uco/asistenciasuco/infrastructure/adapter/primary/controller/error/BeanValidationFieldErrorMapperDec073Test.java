package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** DEC-073: las anotaciones de formato nuevas se reportan como FIELD_INVALID_FORMAT, igual que @Pattern. */
class BeanValidationFieldErrorMapperDec073Test {

    @ParameterizedTest
    @ValueSource(strings = {"NombrePersona", "TextoSeguro", "SinEtiquetasHtml", "NumeroIdentificacionTexto", "Pattern"})
    void las_restricciones_de_formato_se_reportan_como_formato_invalido(final String restriccion) {
        assertEquals(ApiFieldErrorCode.FIELD_INVALID_FORMAT, BeanValidationFieldErrorMapper.toApiCode(restriccion));
    }
}
