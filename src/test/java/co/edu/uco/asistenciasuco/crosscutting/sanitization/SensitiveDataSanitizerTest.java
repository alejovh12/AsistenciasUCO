package co.edu.uco.asistenciasuco.crosscutting.sanitization;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataSanitizerTest {

    @Test
    void sanitizeForLog_con_valor_nulo_retorna_nulo() {
        assertNull(SensitiveDataSanitizer.sanitizeForLog(null));
    }

    @Test
    void sanitizeForLog_trunca_valores_muy_largos() {
        final String largo = "a".repeat(600);

        final String sanitized = SensitiveDataSanitizer.sanitizeForLog(largo);

        assertTrue(sanitized.length() <= 512);
        assertTrue(sanitized.endsWith("..."));
    }

    @Test
    void sanitizeForLog_con_maxLength_cero_retorna_vacio() {
        assertEquals("", SensitiveDataSanitizer.sanitizeForLog("hola", 0));
    }

    @Test
    void sanitizeForLog_con_maxLength_pequeno_usa_puntos() {
        assertEquals("..", SensitiveDataSanitizer.sanitizeForLog("hola mundo", 2));
    }

    @Test
    void sanitizePublicMessage_con_mensaje_en_blanco_retorna_fallback() {
        assertEquals("fallback", SensitiveDataSanitizer.sanitizePublicMessage(null, "fallback"));
        assertEquals("fallback", SensitiveDataSanitizer.sanitizePublicMessage("   ", "fallback"));
    }

    @Test
    void sanitizePublicMessage_con_mensaje_valido_lo_retorna_sanitizado() {
        assertEquals("hola mundo", SensitiveDataSanitizer.sanitizePublicMessage("hola mundo", "fallback"));
    }

    @Test
    void sanitizeMetadata_con_mapa_nulo_o_vacio_retorna_vacio() {
        assertTrue(SensitiveDataSanitizer.sanitizeMetadata(null).isEmpty());
        assertTrue(SensitiveDataSanitizer.sanitizeMetadata(Map.of()).isEmpty());
    }

    @Test
    void sanitizeMetadata_omite_entradas_con_llave_en_blanco() {
        final Map<String, String> conLlaveBlanca = new HashMap<>();
        conLlaveBlanca.put("   ", "valor");

        final Map<String, String> resultado = SensitiveDataSanitizer.sanitizeMetadata(conLlaveBlanca);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void isSensitiveKey_detecta_variantes_de_llaves_sensibles() {
        assertFalse(SensitiveDataSanitizer.isSensitiveKey(null));
        assertFalse(SensitiveDataSanitizer.isSensitiveKey("   "));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("Authorization"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("user-password"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("passwd"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("access-token"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("secret"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("cookie"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("credential"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("api-key"));
        assertTrue(SensitiveDataSanitizer.isSensitiveKey("apikey"));
        assertFalse(SensitiveDataSanitizer.isSensitiveKey("path"));
    }

    @Test
    void sanitizeForLog_elimina_saltos_de_linea_y_redacta_secretos() {
        final String sanitized = SensitiveDataSanitizer.sanitizeForLog(
                "correo=a@uco.edu\r\nAuthorization: Bearer token123 password=abc"
        );

        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("token123"));
        assertFalse(sanitized.contains("abc"));
    }

    @Test
    void sanitizeMetadata_redacta_valores_con_llaves_sensibles() {
        final Map<String, String> metadata = SensitiveDataSanitizer.sanitizeMetadata(Map.of(
                "Authorization", "Bearer secreto",
                "path", "/api/v1/usuarios"
        ));

        assertEquals(SensitiveDataSanitizer.REDACTED, metadata.get(SensitiveDataSanitizer.REDACTED));
        assertEquals("/api/v1/usuarios", metadata.get("path"));
    }
}
