package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearUsuarioRequestValidatorTest {

    private final CrearUsuarioRequestValidator validator = new CrearUsuarioRequestValidator();

    @Test
    void acceptsValidRequiredFieldsAndOptionalPasswordAndNames() {
        final CrearUsuarioRequest request = validRequest();
        request.setPrimerNombre("  María   José  ");
        request.setSegundoNombre("Ana");
        request.setSegundoApellido("López");
        request.setCorreo("  persona@example.com  ");
        request.setPassword(null);

        assertTrue(validator.validate(request).isValid());

        request.setPassword("clave-segura");
        assertTrue(validator.validate(request).isValid());
    }

    @Test
    void rejectsMissingRequestAndRequiredFields() {
        assertIssue(validator.validate(null), "request", ValidationErrorType.REQUIRED);

        final ValidationResult result = validator.validate(new CrearUsuarioRequest());
        assertIssue(result, "tipoIdIdentificacion", ValidationErrorType.REQUIRED);
        assertIssue(result, "numeroIdentificacion", ValidationErrorType.REQUIRED);
        assertIssue(result, "primerNombre", ValidationErrorType.REQUIRED);
        assertIssue(result, "primerApellido", ValidationErrorType.REQUIRED);
        assertIssue(result, "correo", ValidationErrorType.REQUIRED);
    }

    @Test
    void validatesIdentificationRangeAndUuid() {
        final CrearUsuarioRequest request = validRequest();
        request.setTipoIdIdentificacion(new UUID(0, 0));
        request.setNumeroIdentificacion(-1);
        assertIssue(validator.validate(request), "tipoIdIdentificacion", ValidationErrorType.INVALID_UUID);
        assertIssue(validator.validate(request), "numeroIdentificacion", ValidationErrorType.OUT_OF_RANGE);

        request.setTipoIdIdentificacion(UUID.randomUUID());
        request.setNumeroIdentificacion(12345);
        assertIssue(validator.validate(request), "numeroIdentificacion", ValidationErrorType.OUT_OF_RANGE);

        request.setNumeroIdentificacion(123456);
        assertTrue(validator.validate(request).isValid());
    }

    @Test
    void rejectsMalformedAndOversizedNamesAndEmail() {
        final CrearUsuarioRequest request = validRequest();
        request.setPrimerNombre("Juan3");
        request.setPrimerApellido("A".repeat(51));
        request.setSegundoNombre("@ana");
        request.setSegundoApellido("A".repeat(51));
        request.setCorreo("no-es-un-correo");

        final ValidationResult result = validator.validate(request);
        assertIssue(result, "primerNombre", ValidationErrorType.INVALID_FORMAT);
        assertIssue(result, "primerApellido", ValidationErrorType.INVALID_LENGTH);
        assertIssue(result, "segundoNombre", ValidationErrorType.INVALID_FORMAT);
        assertIssue(result, "segundoApellido", ValidationErrorType.INVALID_LENGTH);
        assertIssue(result, "correo", ValidationErrorType.INVALID_FORMAT);

        request.setCorreo("a".repeat(101) + "@example.com");
        assertIssue(validator.validate(request), "correo", ValidationErrorType.INVALID_LENGTH);
    }

    @Test
    void rejectsPasswordOutsideAllowedLength() {
        final CrearUsuarioRequest request = validRequest();
        request.setPassword("corta");
        assertIssue(validator.validate(request), "password", ValidationErrorType.INVALID_LENGTH);

        request.setPassword("x".repeat(256));
        assertIssue(validator.validate(request), "password", ValidationErrorType.INVALID_LENGTH);

        request.setPassword("x".repeat(255));
        assertTrue(validator.validate(request).isValid());
    }

    private static CrearUsuarioRequest validRequest() {
        final CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setTipoIdIdentificacion(UUID.randomUUID());
        request.setNumeroIdentificacion(12345678);
        request.setPrimerNombre("Juan");
        request.setPrimerApellido("Pérez");
        request.setCorreo("persona@example.com");
        return request;
    }

    private static void assertIssue(final ValidationResult result, final String field, final ValidationErrorType type) {
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(issue -> field.equals(issue.field()) && type == issue.type()),
                () -> "No se encontró " + field + "/" + type + " en " + result.issues());
    }
}
