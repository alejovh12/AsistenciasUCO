package co.edu.uco.asistenciasuco.crosscutting.sanitization;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveDataSanitizerTest {

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
