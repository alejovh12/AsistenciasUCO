package co.edu.uco.asistenciasuco.application.features.alertainasistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** CULM-001 C.1 (HU026C): los codigos ALR_* publicados llevan su codigo estable, un texto de respaldo y el tipo de error HTTP. */
class AlertaInasistenciaErrorCodeTest {

    @Test
    void cada_codigo_expone_su_nombre_como_codigo_estable_y_es_de_validacion() {
        for (final AlertaInasistenciaErrorCode codigo : AlertaInasistenciaErrorCode.values()) {
            assertEquals(codigo.name(), codigo.code());
            assertFalse(codigo.defaultMessage().isBlank());
            assertEquals(ErrorKind.VALIDATION, codigo.kind());
        }
    }

    @Test
    void los_textos_de_respaldo_son_los_del_catalogo_institucional() {
        assertEquals("La lista de alertas no es válida.", AlertaInasistenciaErrorCode.ALR_004.defaultMessage());
        assertEquals("La matrícula indicada no pertenece al grupo.", AlertaInasistenciaErrorCode.ALR_006.defaultMessage());
        assertEquals(AlertaInasistenciaErrorCode.ALR_004, AlertaInasistenciaErrorCode.valueOf("ALR_004"));
    }
}
