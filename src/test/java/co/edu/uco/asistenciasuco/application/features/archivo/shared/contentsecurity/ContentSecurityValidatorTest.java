package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RED causal LB-004B.2: CONTENT-001..006 (docs/work-items/LB-004-stateless-serverless-readiness/TEST_PLAN.md).
 */
class ContentSecurityValidatorTest {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    // CONTENT-001
    @Test
    void pdf_valido_es_aceptado() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido pdf");

        final ValidatedContent result = validate("soporte.pdf", content);

        assertEquals("soporte.pdf", result.sanitizedFilename());
        assertEquals("pdf", result.extension());
        assertEquals("application/pdf", result.verifiedContentType());
    }

    // CONTENT-002
    @Test
    void png_valido_es_aceptado() {
        final byte[] content = withPayload(PNG_MAGIC, "imagen");

        final ValidatedContent result = validate("captura.png", content);

        assertEquals("image/png", result.verifiedContentType());
    }

    // CONTENT-003
    @Test
    void jpeg_valido_es_aceptado() {
        final byte[] content = withPayload(JPEG_MAGIC, "imagen");

        final ValidatedContent result = validate("captura.jpg", content);

        assertEquals("image/jpeg", result.verifiedContentType());
    }

    // CONTENT-004
    @Test
    void extension_permitida_con_magic_bytes_incorrectos_es_rechazada() {
        final byte[] content = "MZ ejecutable disfrazado de pdf".getBytes();

        assertThrows(ValidationException.class, () -> validate("script.pdf", content));
    }

    @Test
    void extension_no_permitida_es_rechazada() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido");

        assertThrows(ValidationException.class,
                () -> ContentSecurityValidator.validate("script.exe", "application/octet-stream", content));
    }

    // CONTENT-005
    @Test
    void archivo_vacio_es_rechazado() {
        assertThrows(ValidationException.class, () -> validate("soporte.pdf", new byte[0]));
    }

    @Test
    void archivo_de_exactamente_5_mib_es_aceptado() {
        final byte[] content = withPayloadOfSize(PDF_MAGIC, (int) ContentSecurityValidator.MAX_FILE_SIZE_BYTES);

        final ValidatedContent result = validate("soporte.pdf", content);

        assertEquals("pdf", result.extension());
    }

    @Test
    void archivo_mayor_a_5_mib_es_rechazado() {
        final byte[] content = withPayloadOfSize(PDF_MAGIC, (int) ContentSecurityValidator.MAX_FILE_SIZE_BYTES + 1);

        assertThrows(ValidationException.class, () -> validate("soporte.pdf", content));
    }

    // CONTENT-006
    @ParameterizedTest
    @ValueSource(strings = {"../soporte.pdf", "..\\soporte.pdf", "/tmp/soporte.pdf", "C:\\soporte.pdf", "a..b.pdf"})
    void filename_con_path_traversal_es_rechazado(final String nombre) {
        final byte[] content = withPayload(PDF_MAGIC, "contenido");

        assertThrows(ValidationException.class, () -> validate(nombre, content));
    }

    @Test
    void filename_nulo_usa_valor_por_defecto_y_rechaza_por_falta_de_extension() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido");

        assertThrows(ValidationException.class,
                () -> ContentSecurityValidator.validate(null, "application/pdf", content));
    }

    @Test
    void filename_con_espacios_y_caracteres_especiales_se_sanitiza() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido");

        final ValidatedContent result = validate("soporte médico (1).pdf", content);

        assertEquals("soporte_m_dico__1_.pdf", result.sanitizedFilename());
    }

    // CONTENT-007
    @Test
    void pdf_declarado_como_png_es_rechazado_aunque_magic_y_extension_sean_pdf() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido pdf");

        assertThrows(ValidationException.class,
                () -> ContentSecurityValidator.validate("soporte.pdf", "image/png", content));
    }

    // CONTENT-008
    @Test
    void png_declarado_como_pdf_es_rechazado_aunque_magic_y_extension_sean_png() {
        final byte[] content = withPayload(PNG_MAGIC, "imagen");

        assertThrows(ValidationException.class,
                () -> ContentSecurityValidator.validate("captura.png", "application/pdf", content));
    }

    // CONTENT-009
    @Test
    void jpeg_con_mime_y_magic_correctos_es_aceptado() {
        final byte[] content = withPayload(JPEG_MAGIC, "imagen");

        final ValidatedContent result = ContentSecurityValidator.validate(
                "captura.jpeg", "image/jpeg", content);

        assertEquals("image/jpeg", result.verifiedContentType());
    }

    @Test
    void mime_declarado_ausente_es_rechazado() {
        final byte[] content = withPayload(PDF_MAGIC, "contenido pdf");

        assertThrows(ValidationException.class,
                () -> ContentSecurityValidator.validate("soporte.pdf", null, content));
    }

    private static ValidatedContent validate(final String filename, final byte[] content) {
        final String declaredContentType;
        if (filename != null && filename.toLowerCase().endsWith(".png")) {
            declaredContentType = "image/png";
        } else if (filename != null
                && (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg"))) {
            declaredContentType = "image/jpeg";
        } else {
            declaredContentType = "application/pdf";
        }
        return ContentSecurityValidator.validate(filename, declaredContentType, content);
    }

    private static byte[] withPayload(final byte[] magic, final String payload) {
        final byte[] payloadBytes = payload.getBytes();
        final byte[] content = new byte[magic.length + payloadBytes.length];
        System.arraycopy(magic, 0, content, 0, magic.length);
        System.arraycopy(payloadBytes, 0, content, magic.length, payloadBytes.length);
        return content;
    }

    private static byte[] withPayloadOfSize(final byte[] magic, final int totalSize) {
        final byte[] content = new byte[totalSize];
        Arrays.fill(content, (byte) 'A');
        System.arraycopy(magic, 0, content, 0, magic.length);
        return content;
    }
}


