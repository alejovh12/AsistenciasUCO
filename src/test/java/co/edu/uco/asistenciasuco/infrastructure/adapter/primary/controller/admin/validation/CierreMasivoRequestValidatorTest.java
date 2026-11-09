package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.CierreMasivoRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 D (F-ADM-026): el periodo del cierre masivo es obligatorio. */
class CierreMasivoRequestValidatorTest {

    private final CierreMasivoRequestValidator validator = new CierreMasivoRequestValidator();

    @Test
    void un_periodo_valido_pasa() {
        assertTrue(validator.validate(new CierreMasivoRequest(UUID.randomUUID())).isValid());
    }

    @Test
    void el_cuerpo_nulo_el_periodo_nulo_y_el_uuid_vacio_se_rechazan_como_requeridos() {
        for (final CierreMasivoRequest request : new CierreMasivoRequest[]{null, new CierreMasivoRequest(null),
                new CierreMasivoRequest(new UUID(0L, 0L))}) {
            final ValidationResult resultado = validator.validate(request);
            assertTrue(resultado.hasErrors());
            assertEquals("idPeriodoAcademico", resultado.issues().getFirst().field());
            assertEquals(ValidationErrorType.REQUIRED, resultado.issues().getFirst().type());
        }
    }
}
