package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** ATA D7: {@code CAL_001} (rango invalido del calendario de sesiones) se expone tal cual como validacion (400). */
class DbFailureClassifierCalendarioTest {

    @Test
    void cal_001_se_clasifica_como_validacion_con_su_codigo() {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=CAL_001|rango invalido", "consultarCalendarioDocente");
        assertEquals("CAL_001", clasificado.code());
        assertEquals(ErrorKind.VALIDATION, clasificado.kind());
    }

    @Test
    void cal_001_es_una_excepcion_de_validacion_con_el_codigo_de_la_db() {
        final ValidationException error = assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=CAL_001|rango invalido", "corr", "consultarCalendarioEstudiante"));
        assertEquals("CAL_001", error.getCode());
    }
}
