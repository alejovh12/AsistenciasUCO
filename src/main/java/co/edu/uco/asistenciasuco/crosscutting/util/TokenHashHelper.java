package co.edu.uco.asistenciasuco.crosscutting.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * IDENT-001: utilidad unica del hash de tokens de un solo uso. La emision del token y su verificacion calculan el
 * mismo SHA-256 sobre el token tal cual se entrega al usuario; en la base de datos solo se guarda el hash.
 */
public final class TokenHashHelper {

    /** Longitud del token: 32 bytes aleatorios en base64url sin relleno. */
    public static final int LONGITUD_TOKEN = 43;

    private TokenHashHelper() {
    }

    public static byte[] sha256(final String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token es obligatorio para calcular su hash.");
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM.", exception);
        }
    }

    /** {@code true} solo para 43 caracteres del alfabeto base64url ({@code A-Z a-z 0-9 - _}). */
    public static boolean esFormatoValido(final String token) {
        if (token == null || token.length() != LONGITUD_TOKEN) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            final char c = token.charAt(i);
            final boolean valido = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            if (!valido) {
                return false;
            }
        }
        return true;
    }
}
