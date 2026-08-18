package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringPasswordEncoderAdapterTest {

    @Test
    void encode_genera_hash_adaptativo_verificable_sin_retornar_texto_original() {
        final SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter();
        final String rawPassword = "PasswordValida123!";

        final String firstEncoded = adapter.encode(rawPassword);
        final String secondEncoded = adapter.encode(rawPassword);

        assertNotEquals(rawPassword, firstEncoded);
        assertNotEquals(rawPassword, secondEncoded);
        assertNotEquals(firstEncoded, secondEncoded);
        assertTrue(adapter.matches(rawPassword, firstEncoded));
        assertTrue(adapter.matches(rawPassword, secondEncoded));
        assertFalse(adapter.matches("PasswordIncorrecta123!", firstEncoded));
    }
}
