package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.ActualizarFacultadRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.CrearFacultadRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validacion de forma de crear y actualizar facultad; el resto de reglas (unicidad, ambito) las fuerza la DB. */
class FacultadRequestValidatorTest {

    private final FacultadRequestValidator validator = new FacultadRequestValidator();

    private static boolean tiene(final ValidationResult resultado, final String campo, final ValidationErrorType tipo) {
        return resultado.issues().stream().anyMatch(i -> i.field().equals(campo) && i.type() == tipo);
    }

    @Test
    void crear_con_cuerpo_nulo_es_requerido() {
        assertTrue(tiene(validator.validarCreacion(null), "request", ValidationErrorType.REQUIRED));
    }

    @Test
    void crear_valida_nombre_y_decano_titular() {
        final UUID decano = UUID.randomUUID();

        assertTrue(validator.validarCreacion(new CrearFacultadRequest("Ingenieria", "ING", UUID.randomUUID(), decano)).isValid());
        assertTrue(tiene(validator.validarCreacion(new CrearFacultadRequest("  ", "ING", null, decano)), "nombre", ValidationErrorType.REQUIRED));
        assertTrue(tiene(validator.validarCreacion(new CrearFacultadRequest("ab", "ING", null, decano)), "nombre", ValidationErrorType.INVALID_LENGTH));
        assertTrue(tiene(validator.validarCreacion(new CrearFacultadRequest("x".repeat(51), "ING", null, decano)), "nombre", ValidationErrorType.INVALID_LENGTH));
        assertTrue(tiene(validator.validarCreacion(new CrearFacultadRequest("Ingenieria", "ING", null, null)), "idDecano", ValidationErrorType.REQUIRED));
        assertTrue(tiene(validator.validarCreacion(new CrearFacultadRequest("Ingenieria", "ING", null, new UUID(0L, 0L))), "idDecano", ValidationErrorType.REQUIRED));
    }

    @Test
    void actualizar_con_cuerpo_nulo_es_requerido() {
        assertTrue(tiene(validator.validarActualizacion(null), "request", ValidationErrorType.REQUIRED));
    }

    @Test
    void actualizar_exige_al_menos_nombre_o_decano_y_valida_el_largo_del_nombre() {
        assertTrue(tiene(validator.validarActualizacion(new ActualizarFacultadRequest(" ", null)), "nombre", ValidationErrorType.REQUIRED));
        assertTrue(validator.validarActualizacion(new ActualizarFacultadRequest(null, UUID.randomUUID())).isValid());
        assertTrue(validator.validarActualizacion(new ActualizarFacultadRequest("Ciencias", null)).isValid());
        assertTrue(tiene(validator.validarActualizacion(new ActualizarFacultadRequest("ab", UUID.randomUUID())), "nombre", ValidationErrorType.INVALID_LENGTH));
        assertFalse(validator.validarActualizacion(new ActualizarFacultadRequest("x".repeat(51), null)).isValid());
        assertEquals(1, validator.validarActualizacion(new ActualizarFacultadRequest("x".repeat(51), null)).issues().size());
    }
}
