package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RED causal LB-004B.2: COMPRESSION-001/002
 * (docs/work-items/LB-004-stateless-serverless-readiness/TEST_PLAN.md,
 * CONTENT_SECURITY.md §Política de compresión).
 */
class CompressionPolicyTest {

    // COMPRESSION-001: contenido no beneficiado (simula PDF/PNG/JPEG ya comprimidos: alta entropia).
    @Test
    void contenido_de_alta_entropia_no_se_comprime() {
        final byte[] highEntropyContent = randomBytes(64_000);

        final CompressionDecision decision = CompressionPolicy.evaluate(highEntropyContent);

        assertFalse(decision.compressed());
        assertNull(decision.algorithm());
        assertArrayEquals(highEntropyContent, decision.storedContent());
    }

    // COMPRESSION-002: contenido compresible dentro de los tipos permitidos; round-trip exacto.
    @Test
    void contenido_compresible_se_comprime_y_el_roundtrip_recupera_bytes_originales() {
        final byte[] compressibleContent = repeatedText(64_000);

        final CompressionDecision decision = CompressionPolicy.evaluate(compressibleContent);

        assertTrue(decision.compressed());
        assertTrue(decision.storedContent().length < compressibleContent.length);
        assertArrayEquals(compressibleContent, CompressionPolicy.decompress(decision.storedContent(), decision.algorithm()));
    }

    @Test
    void decompress_con_algoritmo_nulo_retorna_el_contenido_tal_cual() {
        final byte[] content = "sin compresion".getBytes();

        assertArrayEquals(content, CompressionPolicy.decompress(content, null));
    }

    private static byte[] randomBytes(final int size) {
        final byte[] content = new byte[size];
        new SecureRandom().nextBytes(content);
        return content;
    }

    private static byte[] repeatedText(final int size) {
        final byte[] content = new byte[size];
        Arrays.fill(content, (byte) 'A');
        return content;
    }
}


