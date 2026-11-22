package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Revision final P3: BE-1: ASI_014 (asignatura sin vinculo activo a un plan activo de los programas del coordinador) llegaba como 500 de la previsualizacion.
 */
class DbExceptionTranslatorAsi014Test {

    @Test
    void asi_014_de_un_sp_de_consulta_es_409_con_su_propio_codigo() {
        final var traducida = DbExceptionTranslator.translateDbCode("ASI_014", null);

        final ConflictException exception = (ConflictException) traducida.orElseThrow();
        assertEquals("ASI_014", exception.getCode());
    }

    @Test
    void asi_014_de_un_sp_de_comando_es_409_y_conserva_el_mensaje_de_la_db() {
        final String mensaje = "La asignatura no tiene un vínculo activo con un plan activo de los programas que coordinas.";
        final ConflictException exception = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=ASI_014|sin vinculo", "corr", "crearGrupo"));

        assertEquals("ASI_014", exception.getCode());
        assertEquals(mensaje, exception.getMessage());
    }
}
