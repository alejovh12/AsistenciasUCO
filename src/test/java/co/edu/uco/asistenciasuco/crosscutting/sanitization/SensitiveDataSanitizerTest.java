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
    void sanitizeForLog_reemplaza_cr_y_lf_y_redacta_secretos() {
        final String sanitized = SensitiveDataSanitizer.sanitizeForLog("linea1\r\nlinea2\npassword=hunter2");

        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("hunter2"));
        assertTrue(sanitized.startsWith("linea1__linea2_"));
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

    // ------------------------------------------------------------ OBS-06: datos personales

    @Test
    void sanitizeForLog_oculta_el_usuario_del_correo_y_conserva_el_dominio() {
        final String sanitized = SensitiveDataSanitizer.sanitizeForLog(
                "No fue posible crear la cuenta juan.perez@uco.edu.co (ya existe ANA_GOMEZ+test@gmail.com)");

        assertFalse(sanitized.contains("juan.perez"));
        assertFalse(sanitized.contains("ANA_GOMEZ"));
        assertTrue(sanitized.contains("j***@uco.edu.co"));
        assertTrue(sanitized.contains("A***@gmail.com"));
        assertTrue(sanitized.startsWith("No fue posible crear la cuenta "));
    }

    @Test
    void sanitizeForLog_no_toca_arrobas_que_no_son_correos() {
        assertEquals("usuario@ sin dominio y @mencion",
                SensitiveDataSanitizer.sanitizeForLog("usuario@ sin dominio y @mencion"));
    }

    @Test
    void sanitizeForLog_oculta_documento_nombre_y_apellidos_asignados() {
        final String sanitized = SensitiveDataSanitizer.sanitizeForLog(
                "documento=1023456789, numeroDocumento: 98765432; nombre=Juan Carlos, apellidos=Perez Gomez");

        assertFalse(sanitized.contains("1023456789"));
        assertFalse(sanitized.contains("98765432"));
        assertFalse(sanitized.contains("Juan"));
        assertFalse(sanitized.contains("Perez"));
        assertTrue(sanitized.contains("documento=" + SensitiveDataSanitizer.PERSONAL_DATA));
        assertTrue(sanitized.contains("numeroDocumento: " + SensitiveDataSanitizer.PERSONAL_DATA));
        assertTrue(sanitized.contains("nombre=" + SensitiveDataSanitizer.PERSONAL_DATA));
    }

    @Test
    void sanitizeForLog_oculta_datos_personales_en_json() {
        final String sanitized = SensitiveDataSanitizer.sanitizeForLog(
                "{\"nombreCompleto\":\"Ana Maria Gomez\",\"correo\":\"ana@uco.edu.co\",\"grupoId\":\"g-1\"}");

        assertFalse(sanitized.contains("Ana Maria"));
        assertFalse(sanitized.contains("ana@"));
        assertTrue(sanitized.contains("\"grupoId\":\"g-1\""));
    }

    @Test
    void sanitizeForLog_no_oculta_llaves_que_solo_empiezan_como_un_dato_personal() {
        assertEquals("nombreMateria=Calculo, documentoSoporteId=abc",
                SensitiveDataSanitizer.sanitizeForLog("nombreMateria=Calculo, documentoSoporteId=abc"));
    }

    @Test
    void sanitizeForLog_resiste_entradas_largas_sin_retroceso_catastrofico() {
        final String entrada = "a".repeat(50_000) + "@" + "b.".repeat(20_000) + "!";

        final long inicio = System.nanoTime();
        SensitiveDataSanitizer.sanitizeForLog(entrada);
        final long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(milisegundos < 2_000, "la sanitizacion tardo " + milisegundos + " ms");
    }

    @Test
    void isPersonalDataKey_detecta_llaves_exactas_de_datos_personales() {
        assertFalse(SensitiveDataSanitizer.isPersonalDataKey(null));
        assertFalse(SensitiveDataSanitizer.isPersonalDataKey("  "));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("correo"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("Email"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("correo_institucional"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("numero-documento"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("numeroIdentificacion"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("cedula"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("nombres"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("apellido"));
        assertTrue(SensitiveDataSanitizer.isPersonalDataKey("nombreCompleto"));
        assertFalse(SensitiveDataSanitizer.isPersonalDataKey("nombreMateria"));
        assertFalse(SensitiveDataSanitizer.isPersonalDataKey("path"));
    }

    @Test
    void sanitizeMetadata_oculta_el_valor_de_datos_personales_y_conserva_la_llave() {
        final Map<String, String> metadata = SensitiveDataSanitizer.sanitizeMetadata(Map.of(
                "correo", "ana@uco.edu.co",
                "numeroDocumento", "1023456789",
                "nombreCompleto", "Ana Gomez",
                "path", "/api/v1/docente/grupos"
        ));

        assertEquals(SensitiveDataSanitizer.PERSONAL_DATA, metadata.get("correo"));
        assertEquals(SensitiveDataSanitizer.PERSONAL_DATA, metadata.get("numeroDocumento"));
        assertEquals(SensitiveDataSanitizer.PERSONAL_DATA, metadata.get("nombreCompleto"));
        assertEquals("/api/v1/docente/grupos", metadata.get("path"));
    }

    @Test
    void sanitizeMetadata_oculta_correos_dentro_de_valores_libres() {
        final Map<String, String> metadata = SensitiveDataSanitizer.sanitizeMetadata(Map.of(
                "detalle", "destinatario juan@uco.edu.co rechazado"));

        assertEquals("destinatario j***@uco.edu.co rechazado", metadata.get("detalle"));
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
