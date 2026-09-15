package co.edu.uco.asistenciasuco.crosscutting.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectHelperTest {

    @Test
    void isNull_distingue_valores_nulos_y_no_nulos() {
        assertTrue(ObjectHelper.isNull(null));
        assertFalse(ObjectHelper.isNull("valor"));
    }

    @Test
    void isNotNull_distingue_valores_nulos_y_no_nulos() {
        assertFalse(ObjectHelper.isNotNull(null));
        assertTrue(ObjectHelper.isNotNull("valor"));
    }

    @Test
    void requireNonNull_con_valor_nulo_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class, () -> ObjectHelper.requireNonNull(null, "mensaje"));
    }

    @Test
    void requireNonNull_con_valor_valido_lo_retorna() {
        assertEquals("valor", ObjectHelper.requireNonNull("valor", "mensaje"));
    }

    @Test
    void requireNotEmptyUuid_con_valor_nulo_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> ObjectHelper.requireNotEmptyUuid(null, "nulo", "vacio"));
    }

    @Test
    void requireNotEmptyUuid_con_uuid_vacio_lanza_excepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> ObjectHelper.requireNotEmptyUuid(new UUID(0L, 0L), "nulo", "vacio"));
    }

    @Test
    void requireNotEmptyUuid_con_uuid_valido_lo_retorna() {
        final UUID uuid = UUID.randomUUID();

        assertEquals(uuid, ObjectHelper.requireNotEmptyUuid(uuid, "nulo", "vacio"));
    }
}
