package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Politica de compresion: "si se puede comprimir, se comprima" interpretado como evaluacion
 * explicita y testeable, no como compresion cosmetica forzada. Ver
 * docs/work-items/LB-004-stateless-serverless-readiness/CONTENT_SECURITY.md.
 *
 * <p>PDF/PNG/JPEG ya suelen estar comprimidos internamente: para la mayoria de soportes reales
 * esta evaluacion concluira {@code compressed=false} (NO_BENEFIT / STORE_ORIGINAL), lo cual es el
 * resultado correcto, no un caso sin cubrir.</p>
 */
public final class CompressionPolicy {

    public static final String ALGORITHM_DEFLATE = "DEFLATE";

    /** Ahorro minimo para considerar la compresion materialmente beneficiosa. */
    private static final double MIN_SAVINGS_RATIO = 0.10;

    private CompressionPolicy() {
    }

    public static CompressionDecision evaluate(final byte[] content) {
        final byte[] candidate = deflate(content);
        final boolean strictlySmaller = candidate.length < content.length;
        final double savingsRatio = strictlySmaller
                ? 1.0 - ((double) candidate.length / (double) content.length)
                : 0.0;

        if (strictlySmaller && savingsRatio >= MIN_SAVINGS_RATIO) {
            return CompressionDecision.compressed(ALGORITHM_DEFLATE, candidate);
        }
        return CompressionDecision.original(content);
    }

    public static byte[] decompress(final byte[] storedContent, final String algorithm) {
        if (algorithm == null) {
            return storedContent;
        }
        if (!ALGORITHM_DEFLATE.equals(algorithm)) {
            throw new InternalApplicationException("Algoritmo de compresion no soportado: " + algorithm);
        }
        return inflate(storedContent);
    }

    private static byte[] deflate(final byte[] content) {
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(content.length);
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(buffer, deflater)) {
            deflaterStream.write(content);
        } catch (final java.io.IOException exception) {
            throw new InternalApplicationException("No fue posible evaluar la compresion del archivo.", exception);
        } finally {
            deflater.end();
        }
        return buffer.toByteArray();
    }

    private static byte[] inflate(final byte[] compressedContent) {
        final Inflater inflater = new Inflater();
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(compressedContent.length * 2);
        try (InflaterOutputStream inflaterStream = new InflaterOutputStream(buffer, inflater)) {
            inflaterStream.write(compressedContent);
        } catch (final java.io.IOException exception) {
            throw new InternalApplicationException("No fue posible restaurar el contenido original del archivo.", exception);
        } finally {
            inflater.end();
        }
        return buffer.toByteArray();
    }
}
