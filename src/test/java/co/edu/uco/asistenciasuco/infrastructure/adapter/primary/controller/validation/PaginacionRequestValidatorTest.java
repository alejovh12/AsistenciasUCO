package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CULM-001 D.9-a (CONTRACT §11.1, F-ADM-023/F-EST-022): pagina >= 1 y 1 <= tamano <= tamano maximo, rechazo PAG_001. */
class PaginacionRequestValidatorTest {

    @Test
    void sin_parametros_usa_la_pagina_1_y_el_tamano_por_defecto() {
        final PaginacionRequestValidator.Pagina pagina = PaginacionRequestValidator.resolver(null, null);

        assertEquals(1, pagina.numero());
        assertEquals(10, pagina.tamano());
    }

    @Test
    void acepta_los_limites_inferior_y_superior() {
        assertEquals(new PaginacionRequestValidator.Pagina(1, 1), PaginacionRequestValidator.resolver(1, 1));
        assertEquals(new PaginacionRequestValidator.Pagina(7, 100), PaginacionRequestValidator.resolver(7, 100));
    }

    @Test
    void rechaza_pagina_cero_y_negativa_con_el_campo_pagina() {
        for (final int invalida : new int[]{0, -1, -50}) {
            final RequestValidationException error = assertThrows(RequestValidationException.class,
                    () -> PaginacionRequestValidator.resolver(invalida, 10));

            assertEquals(PaginacionRequestValidator.CODIGO, error.getErrorCode());
            assertEquals(List.of("pagina"), campos(error));
            assertEquals(ValidationErrorType.OUT_OF_RANGE, error.getValidationResult().issues().getFirst().type());
        }
    }

    @Test
    void rechaza_tamano_cero_negativo_y_mayor_al_maximo_con_el_campo_tamano() {
        for (final int invalido : new int[]{0, -1, 101, 100000}) {
            final RequestValidationException error = assertThrows(RequestValidationException.class,
                    () -> PaginacionRequestValidator.resolver(1, invalido));

            assertEquals("PAG_001", error.getErrorCode());
            assertEquals(List.of("tamano"), campos(error));
        }
    }

    @Test
    void informa_los_dos_campos_cuando_ambos_son_invalidos() {
        final RequestValidationException error = assertThrows(RequestValidationException.class,
                () -> PaginacionRequestValidator.resolver(-1, 100000));

        assertEquals(List.of("pagina", "tamano"), campos(error));
    }

    private static List<String> campos(final RequestValidationException error) {
        return error.getValidationResult().issues().stream().map(ValidationIssue::field).toList();
    }
}
