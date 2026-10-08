package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Calcula el checksum SHA-256 del contenido aceptado, para integridad (no autorizacion).
 */
public final class ChecksumCalculator {

    private ChecksumCalculator() {
    }

    public static String sha256(final byte[] content) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (final NoSuchAlgorithmException exception) {
            throw new InternalApplicationException("SHA-256 no disponible en este runtime.", exception);
        }
    }
}
