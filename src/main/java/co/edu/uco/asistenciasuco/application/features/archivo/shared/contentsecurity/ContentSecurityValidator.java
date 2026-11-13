package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Validacion de contenido de soportes de revision: tamano, extension, sanitizacion de nombre y
 * verificacion de magic bytes contra el contenido real (nunca solo extension/Content-Type
 * declarado). Ver docs/work-items/LB-004-stateless-serverless-readiness/CONTENT_SECURITY.md.
 */
public final class ContentSecurityValidator {

    /**
     * Techo tecnico del soporte (5 MiB): limite multipart del servidor y tope de descompresion. PAR-05: el limite vigente
     * es el parametro {@code ARCHIVO/TAMANIO_MAXIMO_BYTES} (rango 256 KiB..5 MiB en el catalogo); este valor es su respaldo.
     */
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    /** Longitud maxima del nombre persistido: coincide con ArchivoSoporte.nombreOriginal NVARCHAR(250). */
    public static final int MAX_FILENAME_LENGTH = 250;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
    );

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );

    private ContentSecurityValidator() {
    }

    public static ValidatedContent validate(
            final String originalFilename,
            final String declaredContentType,
            final byte[] content
    ) {
        return validate(originalFilename, declaredContentType, content, MAX_FILE_SIZE_BYTES);
    }

    /**
     * PAR-05: valida con el tamano maximo vigente (parametro del catalogo). Un limite fuera de (0, techo] se ignora y se
     * aplica el techo tecnico {@link #MAX_FILE_SIZE_BYTES}: el parametro solo puede bajar el limite, nunca superarlo.
     */
    public static ValidatedContent validate(
            final String originalFilename,
            final String declaredContentType,
            final byte[] content,
            final long maxFileSizeBytes
    ) {
        final long limite = maxFileSizeBytes > 0 && maxFileSizeBytes <= MAX_FILE_SIZE_BYTES ? maxFileSizeBytes : MAX_FILE_SIZE_BYTES;
        if (content == null || content.length == 0) {
            throw new ValidationException("El archivo adjunto no puede estar vacio.");
        }
        if (content.length > limite) {
            throw new ValidationException("El archivo excede el tamano maximo permitido (" + describirTamano(limite) + ").");
        }

        final String safeName = sanitizeFilename(originalFilename);
        final String extension = extensionOf(safeName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                    "Formato de archivo no permitido. Solo se aceptan archivos PDF, PNG o JPG.");
        }

        final String expectedContentType = CONTENT_TYPE_BY_EXTENSION.get(extension);
        if (!expectedContentType.equals(normalizeDeclaredContentType(declaredContentType))) {
            throw new ValidationException(
                    "El Content-Type declarado no coincide con la extension del archivo.");
        }

        if (!matchesMagicBytes(content, MAGIC_BYTES.get(extension))) {
            throw new ValidationException(
                    "El contenido del archivo no coincide con su extension declarada.");
        }

        return new ValidatedContent(safeName, extension, expectedContentType);
    }

    /** {@code 5 MiB}, {@code 512 KiB} o {@code 300000 bytes}: el texto del limite en el mensaje de error. */
    static String describirTamano(final long bytes) {
        final long kib = 1024L;
        final long mib = kib * kib;
        if (bytes % mib == 0) {
            return (bytes / mib) + " MiB";
        }
        if (bytes % kib == 0) {
            return (bytes / kib) + " KiB";
        }
        return bytes + " bytes";
    }

    public static String sanitizeFilename(final String filename) {
        final String candidate = filename == null || filename.isBlank() ? "archivo" : filename;
        if (!isSimpleFilename(candidate)) {
            throw new ValidationException("Nombre de archivo no valido.");
        }
        final String sanitized = candidate.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            throw new ValidationException("El nombre del archivo excede la longitud maxima permitida (250 caracteres).");
        }
        return sanitized;
    }

    private static boolean isSimpleFilename(final String filename) {
        if (filename.isBlank() || filename.equals(".") || filename.contains("..")
                || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0 || filename.indexOf(':') >= 0
                || containsControlCharacters(filename)) {
            return false;
        }
        try {
            final Path path = Paths.get(filename);
            return !path.isAbsolute() && path.getNameCount() == 1;
        } catch (final InvalidPathException exception) {
            return false;
        }
    }

    private static boolean containsControlCharacters(final String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    private static String extensionOf(final String filename) {
        final int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1).toLowerCase();
    }

    private static String normalizeDeclaredContentType(final String declaredContentType) {
        return declaredContentType == null ? "" : declaredContentType.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesMagicBytes(final byte[] content, final byte[] expectedPrefix) {
        if (expectedPrefix == null || content.length < expectedPrefix.length) {
            return false;
        }
        for (int i = 0; i < expectedPrefix.length; i++) {
            if (content[i] != expectedPrefix[i]) {
                return false;
            }
        }
        return true;
    }
}
