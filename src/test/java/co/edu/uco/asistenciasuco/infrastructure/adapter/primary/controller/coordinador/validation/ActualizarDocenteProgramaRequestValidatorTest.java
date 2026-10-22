package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.ActualizarDocenteProgramaRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActualizarDocenteProgramaRequestValidatorTest {

    private final ActualizarDocenteProgramaRequestValidator validator = new ActualizarDocenteProgramaRequestValidator();

    @Test
    void acepta_tipo_de_vinculacion_solo_o_nombres_solos() {
        final ActualizarDocenteProgramaRequest soloTipo = new ActualizarDocenteProgramaRequest();
        soloTipo.setIdTipoVinculacion(UUID.randomUUID());
        assertTrue(validator.validate(soloTipo).isValid());

        final ActualizarDocenteProgramaRequest soloNombres = new ActualizarDocenteProgramaRequest();
        soloNombres.setPrimerNombre("Ana");
        soloNombres.setSegundoApellido("");
        assertTrue(validator.validate(soloNombres).isValid());
    }

    @Test
    void T_092_03_rechaza_request_nulo_o_sin_ningun_campo() {
        assertIssue(validator.validate(null), "request", ValidationErrorType.REQUIRED);
        assertIssue(validator.validate(new ActualizarDocenteProgramaRequest()), "campos", ValidationErrorType.REQUIRED);
    }

    @Test
    void rechaza_uuid_vacio_nombres_obligatorios_en_blanco_y_longitudes() {
        final ActualizarDocenteProgramaRequest request = new ActualizarDocenteProgramaRequest();
        request.setIdTipoVinculacion(new UUID(0, 0));
        request.setPrimerNombre("   ");
        request.setPrimerApellido("x".repeat(51));
        request.setSegundoNombre("y".repeat(51));
        final ValidationResult result = validator.validate(request);

        assertIssue(result, "idTipoVinculacion", ValidationErrorType.INVALID_UUID);
        assertIssue(result, "primerNombre", ValidationErrorType.REQUIRED);
        assertIssue(result, "primerApellido", ValidationErrorType.INVALID_LENGTH);
        assertIssue(result, "segundoNombre", ValidationErrorType.INVALID_LENGTH);
    }

    private static void assertIssue(final ValidationResult result, final String field, final ValidationErrorType type) {
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(issue -> field.equals(issue.field()) && type == issue.type()),
                () -> "No se encontró " + field + "/" + type + " en " + result.issues());
    }
}
