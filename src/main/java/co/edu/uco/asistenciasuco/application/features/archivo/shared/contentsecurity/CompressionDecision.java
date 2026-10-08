package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

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
}
