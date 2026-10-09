package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas candidatas RED, QUALITY-PR15 F05.
 * 5 MiB procede del contrato CONTENT_SECURITY de LB-004.
 * No demuestra un DoS real ni reemplaza pruebas del pipeline completo.
 */
class CompressionPolicyExpansionBoundaryTest {

    private static final int MAX_ORIGINAL_BYTES = 5 * 1024 * 1024;

    @Test
    void deflate_de_tamano_permitido_se_restaura_sin_perder_un_byte() {
        final byte[] original = repeatedBytes(MAX_ORIGINAL_BYTES);
        final CompressionDecision compressed = CompressionPolicy.evaluate(original);

        assertTrue(compressed.compressed(), "El fixture repetitivo debe comprimirse.");
        assertArrayEquals(original,
                CompressionPolicy.decompress(compressed.storedContent(), compressed.algorithm()));
    }

    @Test
    void deflate_que_expande_a_mas_de_cinco_mib_se_rechaza() {
        final byte[] originalExcesivo = repeatedBytes(MAX_ORIGINAL_BYTES + 1);
        final CompressionDecision compressed = CompressionPolicy.evaluate(originalExcesivo);

        assertTrue(compressed.compressed(), "La prueba requiere bytes comprimidos de entrada.");
        assertThrows(InternalApplicationException.class,
                () -> CompressionPolicy.decompress(compressed.storedContent(), compressed.algorithm()),
                "Ninguna descarga debe descomprimir un soporte sobre el limite contractual.");
    }

    private static byte[] repeatedBytes(final int size) {
        final byte[] result = new byte[size];
        Arrays.fill(result, (byte) 'A');
        return result;
    }
}
