package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CambiarEstadoDocenteProgramaRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CambiarEstadoDocenteProgramaRequestValidatorTest {

    private final CambiarEstadoDocenteProgramaRequestValidator validator = new CambiarEstadoDocenteProgramaRequestValidator();

    @Test
    void acepta_true_y_false() {
        final CambiarEstadoDocenteProgramaRequest request = new CambiarEstadoDocenteProgramaRequest();
        request.setEstaActivo(true);
        assertTrue(validator.validate(request).isValid());
        request.setEstaActivo(false);
        assertTrue(validator.validate(request).isValid());
    }

    @Test
    void rechaza_request_nulo_y_estaActivo_ausente() {
        assertIssue(validator.validate(null), "request");
        assertIssue(validator.validate(new CambiarEstadoDocenteProgramaRequest()), "estaActivo");
    }

    private static void assertIssue(final ValidationResult result, final String field) {
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(i -> field.equals(i.field()) && i.type() == ValidationErrorType.REQUIRED),
                () -> "No se encontró " + field + " en " + result.issues());
    }
}
