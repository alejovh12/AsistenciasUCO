package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B-06: la traza se registra como throwable estructurado (no como texto del mensaje) y con el mensaje saneado. */
class GlobalExceptionHandlerSanitizedThrowableTest {

    @Test
    void conserva_tipo_y_pila_y_no_incrusta_la_traza_en_el_mensaje() {
        final IllegalStateException original = new IllegalStateException("fallo interno");

        final Throwable copia = GlobalExceptionHandler.sanitizedCopy(original);

        assertTrue(copia.getMessage().startsWith("java.lang.IllegalStateException: fallo interno"));
        assertFalse(copia.getMessage().contains("\tat "));
        assertFalse(copia.getMessage().contains("stackTrace"));
        assertArrayEquals(original.getStackTrace(), copia.getStackTrace());
        assertNull(copia.getCause());
    }

    @Test
    void copia_la_cadena_de_causas_y_sanea_credenciales_del_mensaje() {
        final RuntimeException original = new RuntimeException("externo",
                new IllegalArgumentException("password=SuperSecreto123 token=abc.def.ghi"));

        final Throwable copia = GlobalExceptionHandler.sanitizedCopy(original);

        assertNotNull(copia.getCause());
        assertTrue(copia.getCause().getMessage().contains("IllegalArgumentException"));
        assertFalse(copia.getCause().getMessage().contains("SuperSecreto123"));
        assertEquals(original.getCause().getStackTrace().length, copia.getCause().getStackTrace().length);
    }

    @Test
    void un_mensaje_nulo_no_rompe_la_copia() {
        assertNotNull(GlobalExceptionHandler.sanitizedCopy(new NullPointerException()).getMessage());
    }
}
