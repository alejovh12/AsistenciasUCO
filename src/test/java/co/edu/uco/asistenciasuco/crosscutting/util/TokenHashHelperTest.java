package co.edu.uco.asistenciasuco.crosscutting.util;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** IDENT-001 BE-V04: el SHA-256 del token lo calcula una sola utilidad compartida por la emision y la verificacion. */
class TokenHashHelperTest {

    @Test
    void sha256_coincide_con_el_vector_conocido() {
        final byte[] hash = TokenHashHelper.sha256("abc");

        assertEquals(32, hash.length);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HexFormat.of().formatHex(hash));
    }

    @Test
    void sha256_es_determinista_y_distingue_entradas() {
        assertArrayEquals(TokenHashHelper.sha256("token-1"), TokenHashHelper.sha256("token-1"));
        assertFalse(java.util.Arrays.equals(TokenHashHelper.sha256("token-1"), TokenHashHelper.sha256("token-2")));
    }

    @Test
    void sha256_rechaza_nulo_y_vacio() {
        assertThrows(IllegalArgumentException.class, () -> TokenHashHelper.sha256(null));
        assertThrows(IllegalArgumentException.class, () -> TokenHashHelper.sha256(""));
        assertThrows(IllegalArgumentException.class, () -> TokenHashHelper.sha256("   "));
    }

    @Test
    void esFormatoValido_acepta_43_caracteres_base64url_y_rechaza_el_resto() {
        final String valido = "A".repeat(43);

        assertTrue(TokenHashHelper.esFormatoValido(valido));
        assertTrue(TokenHashHelper.esFormatoValido("abcDEF012-_".repeat(3) + "abcdefghij"));
        assertFalse(TokenHashHelper.esFormatoValido(null));
        assertFalse(TokenHashHelper.esFormatoValido(""));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(42)));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(44)));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(42) + "+"));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(42) + "="));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(42) + " "));
        assertFalse(TokenHashHelper.esFormatoValido("A".repeat(42) + "/"));
    }
}
