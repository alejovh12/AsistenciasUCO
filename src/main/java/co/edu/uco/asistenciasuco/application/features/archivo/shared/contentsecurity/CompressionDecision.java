package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resultado de evaluar si comprimir un contenido resulta materialmente beneficioso.
 */
public record CompressionDecision(boolean compressed, String algorithm, byte[] storedContent) {

    public static CompressionDecision original(final byte[] content) {
        return new CompressionDecision(false, null, content);
    }

    public static CompressionDecision compressed(final String algorithm, final byte[] compressedContent) {
        return new CompressionDecision(true, algorithm, compressedContent);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CompressionDecision that)) {
            return false;
        }
        return compressed == that.compressed
                && Objects.equals(algorithm, that.algorithm)
                && Arrays.equals(storedContent, that.storedContent);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(compressed, algorithm) + Arrays.hashCode(storedContent);
    }

    /** No expone el payload del archivo; solo su longitud. */
    @Override
    public String toString() {
        return "CompressionDecision[compressed=" + compressed + ", algorithm=" + algorithm
                + ", storedContent=" + (storedContent == null ? "null" : storedContent.length + " bytes") + "]";
    }
}
