package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeRowValuesTest {

    private static final UUID IDENTIFICADOR = UUID.fromString("11111111-2222-4333-8444-555555555555");

    @Test
    void uuid_nulo_se_conserva() {
        assertNull(NativeRowValues.uuid(null));
    }

    @Test
    void uuid_existente_se_reutiliza() {
        assertSame(IDENTIFICADOR, NativeRowValues.uuid(IDENTIFICADOR));
    }

    @Test
    void texto_se_normaliza_a_uuid() {
        assertEquals(IDENTIFICADOR, NativeRowValues.uuid("  " + IDENTIFICADOR.toString().toUpperCase() + "  "));
    }

    @Test
    void tipo_no_soportado_falla_de_forma_expresa() {
        assertThrows(IllegalArgumentException.class, () -> NativeRowValues.uuid(42));
    }
}
