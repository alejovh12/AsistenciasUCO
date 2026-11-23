package co.edu.uco.notificaciones.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorreosTest {

    @Test
    void aceptaDireccionesNormales() {
        assertTrue(Correos.esCorreoSeguro("ana@uco.edu.co"));
        assertTrue(Correos.esCorreoSeguro("  ana.perez+tag@uco.edu.co  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "sin-arroba", "@uco.edu.co", "a@b@uco.edu.co", "a@uc", "a@sindominio", "a@.uco.co", "a@uco.co.",
            "a b@uco.edu.co", "a,b@uco.edu.co", "a;b@uco.edu.co", "<a>@uco.edu.co", "\"a\"@uco.edu.co", "(a)@uco.edu.co", "a@uco.edu.co\nBcc: x@y.co"})
    void rechazaDireccionesInseguras(final String correo) {
        assertFalse(Correos.esCorreoSeguro(correo));
    }
}
