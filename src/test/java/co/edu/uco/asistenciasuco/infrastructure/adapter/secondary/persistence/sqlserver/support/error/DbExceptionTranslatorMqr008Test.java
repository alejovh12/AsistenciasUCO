package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * BE-2 (DEC-078): MQR_008 del portal perdia su codigo y su texto en un CONFLICT generico; el frontend necesita el codigo para
 * mostrar el selector de programas (los {@code programasElegibles} los entrega la validacion publica del token).
 */
class DbExceptionTranslatorMqr008Test {

    @Test
    void mqr_008_conserva_su_codigo_y_el_mensaje_de_la_db_en_un_409() {
        final String mensaje = "Este grupo lo comparten varios programas: elige el programa al que perteneces para matricularte.";
        final ConflictException exception = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=MQR_008|programa no elegido", "corr", "matricularEstudiantePorToken"));

        assertEquals("MQR_008", exception.getCode());
        assertEquals(mensaje, exception.getMessage());
    }
}
