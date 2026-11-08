package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorregirIdentidadRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F-ADM-S12: mensajes de validacion amigables para el usuario sin nombres tecnicos de campo en camelCase. */
class CorregirIdentidadRequestValidatorTest {

    private final CorregirIdentidadRequestValidator validator = new CorregirIdentidadRequestValidator();

    @Test
    void primer_apellido_con_caracteres_no_permitidos_tiene_mensaje_amigable() {
        final CorregirIdentidadRequest request = new CorregirIdentidadRequest();
        request.setPrimerNombre("Carlos");
        request.setPrimerApellido("Perez123");
        request.setMotivo("Correccion");

        final ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
        assertTrue(result.issues().stream()
                .anyMatch(i -> "primerApellido".equals(i.field())
                        && "El primer apellido contiene caracteres no permitidos.".equals(i.message())),
                "El mensaje no debe contener el nombre tecnico 'primerApellido': " + result.issues());
    }

    @Test
    void primer_nombre_obligatorio_tiene_mensaje_amigable() {
        final CorregirIdentidadRequest request = new CorregirIdentidadRequest();
        request.setPrimerNombre(" ");
        request.setPrimerApellido("Perez");
        request.setMotivo("Correccion");

        final ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
        assertTrue(result.issues().stream()
                .anyMatch(i -> "primerNombre".equals(i.field())
                        && "El primer nombre es obligatorio.".equals(i.message())),
                "El mensaje debe ser 'El primer nombre es obligatorio.': " + result.issues());
    }

    @Test
    void campos_de_nombre_no_exponen_identificadores_tecnicos() {
        final CorregirIdentidadRequest request = new CorregirIdentidadRequest();
        request.setPrimerNombre("A".repeat(51));
        request.setSegundoNombre("1234");
        request.setPrimerApellido("B".repeat(51));
        request.setSegundoApellido("5678");
        request.setMotivo("Correccion");

        final ValidationResult result = validator.validate(request);
        assertFalse(result.isValid());
        for (final ValidationIssue issue : result.issues()) {
            assertFalse(issue.message().contains("primerNombre"), "No debe contener primerNombre en: " + issue.message());
            assertFalse(issue.message().contains("primerApellido"), "No debe contener primerApellido en: " + issue.message());
            assertFalse(issue.message().contains("segundoNombre"), "No debe contener segundoNombre en: " + issue.message());
            assertFalse(issue.message().contains("segundoApellido"), "No debe contener segundoApellido en: " + issue.message());
        }
    }
}
