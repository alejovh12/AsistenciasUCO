package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * RED causal LB-004B.2: INTEGRITY-001.
 */
class ChecksumCalculatorTest {

    @Test
    void mismo_contenido_produce_el_mismo_checksum() {
        final byte[] content = "contenido identico".getBytes();

        assertEquals(ChecksumCalculator.sha256(content), ChecksumCalculator.sha256(content.clone()));
    }

    @Test
    void contenido_distinto_produce_checksum_distinto() {
        assertNotEquals(
                ChecksumCalculator.sha256("a".getBytes()),
                ChecksumCalculator.sha256("b".getBytes())
        );
    }

    @Test
    void checksum_conocido_de_cadena_vacia() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ChecksumCalculator.sha256(new byte[0])
        );
    }
}


